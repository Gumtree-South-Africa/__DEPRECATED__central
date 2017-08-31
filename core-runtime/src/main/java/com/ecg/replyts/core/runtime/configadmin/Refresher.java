package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Regularly checks for modifications of plugin configurations and informs the responsible {@link ConfigurationAdmin}
 * about this change.
 */
public class Refresher {
    private static final Logger LOG = LoggerFactory.getLogger(Refresher.class);

    private static final long RATE = TimeUnit.MINUTES.toMillis(1);
    private final String threadName;

    @Autowired
    private ConfigurationRepository repository;

    @Autowired(required = false)
    private List<ConfigurationRefreshEventListener> refreshEventListeners = Collections.emptyList();

    private ConfigurationAdmin<Object> admin;

    private final Timer refreshChecker;

    public Refresher(ConfigurationAdmin<Object> admin) {
        this.admin = admin;
        threadName = "configuration-checker-" + admin.getAdminName();
        this.refreshChecker = new Timer(threadName, true);
    }

    @PostConstruct
    public void initialize() {
        LOG.info("Refreshing configurations now and periodically refreshing with daemon thread {} every {} ms", threadName, RATE);

        updateConfigurations();

        refreshChecker.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateConfigurations();
                } catch (RuntimeException e) {
                    LOG.error("Updating Plugin Configurations from Database failed", e);
                }
        } }, RATE, RATE);
    }

    @PreDestroy
    public void destroy() {
        refreshChecker.cancel();
    }

    public void updateConfigurations() {
        Map<ConfigurationId, PluginConfiguration> runningConfigs = admin.getRunningServices().stream()
          .collect(Collectors.toMap((p) -> p.getConfiguration().getId(), (p) -> p.getConfiguration(), (a, b) -> b));

        Map<ConfigurationId, PluginConfiguration> newConfigs = repository.getConfigurations().stream()
          .filter((c) -> admin.handlesConfiguration(c.getId()))
          .collect(Collectors.toMap((c) -> c.getId(), (c) -> c, (a, b) -> b));

        Set<ConfigurationId> configsToBeRemoved = runningConfigs.keySet().stream()
          .filter((c) -> !newConfigs.containsKey(c))
          .collect(Collectors.toSet());

        Set<PluginConfiguration> configsToBeUpdated = newConfigs.entrySet().stream()
          .filter((c) -> {
            PluginConfiguration newConfiguration = c.getValue(), oldConfiguration = runningConfigs.get(c.getKey());

            return oldConfiguration == null || oldConfiguration.getVersion() < newConfiguration.getVersion();
          })
          .map(Map.Entry::getValue)
          .collect(Collectors.toSet());

        for (ConfigurationId toDelete : configsToBeRemoved) {
            for (ConfigurationRefreshEventListener listener : refreshEventListeners) {
                if (listener.isApplicable(toDelete.getPluginFactory())) {
                    try {
                        listener.unregister(toDelete.getInstanceId());
                    } catch (Exception ex) {
                        LOG.warn("Exception occurred during unregistering refresh event listener: " + toDelete, ex);
                    }
                }
            }

            try {
                admin.deleteConfiguration(toDelete);
            } catch (Exception ex) {
                LOG.warn("Exception occurred during deleting the configuration: " + toDelete, ex);
            }
        }

        configsToBeUpdated.forEach((c) -> admin.putConfiguration(c));
    }
}