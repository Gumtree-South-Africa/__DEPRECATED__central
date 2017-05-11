package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * regularly checks for modifications of plugin configurations and informs the responsible {@link ConfigurationAdmin}
 * about this change. Method {@link #start()} needs to be invoked to regularly do the check-
 *
 * @author mhuttar
 */
@Component
class Refresher {
    private static final Logger LOG = LoggerFactory.getLogger(Refresher.class);

    private final ConfigurationRepository repository;
    private final ConfigurationAdmin<Object> admin;

    private Timer refreshChecker;

    private static final long RATE = TimeUnit.MINUTES.toMillis(1);

    @Autowired(required = false)
    private List<ConfigurationRefreshEventListener> refreshEventListeners = Collections.emptyList();

    Refresher(ConfigurationRepository repository, ConfigurationAdmin<Object> admin) {
        this.repository = repository;
        this.admin = admin;
    }

    /**
     * starts regular refreshing. interval is {@link #RATE} in ms. Refreshing is done in a daemon thread.
     */
    public void start() {
        if (refreshChecker == null) {
            String threadname = "configuration-checker-" + admin.getAdminName();
            LOG.info("Starting Configuration Refresher {}. Interval: {} ms", threadname, RATE);
            refreshChecker = new Timer(threadname, true);
            refreshChecker.schedule(new TimerTask() {

                @Override
                public void run() {
                    try {
                        updateConfigurations();
                    } catch (RuntimeException e) {
                        LOG.error("Updating Plugin Configurations from Database failed", e);
                    }
                }
            }, RATE, RATE);
        }
    }

    void updateConfigurations() {
        Map<ConfigurationId, PluginConfiguration> runningConfigs = toMap(admin.getRunningServices());
        Map<ConfigurationId, PluginConfiguration> newConfigs = new HashMap<>();
        for (PluginConfiguration config : repository.getConfigurations()) {
            if (admin.handlesConfiguration(config.getId())) {
                newConfigs.put(config.getId(), config);
            }
        }

        Set<ConfigurationId> configsToBeRemoved = new HashSet<>(runningConfigs.keySet());
        configsToBeRemoved.removeAll(newConfigs.keySet());

        Set<PluginConfiguration> configsToBeUpdated = new HashSet<>();
        for (Map.Entry<ConfigurationId, PluginConfiguration> configEntry : newConfigs.entrySet()) {
            ConfigurationId configId = configEntry.getKey();
            PluginConfiguration newConfiguration = configEntry.getValue();
            PluginConfiguration oldConfiguration = runningConfigs.get(configId);

            if (oldConfiguration == null || oldConfiguration.getVersion() < newConfiguration.getVersion()) {
                configsToBeUpdated.add(newConfiguration);
            }
        }

        for (ConfigurationId toDelete : configsToBeRemoved) {
            refreshEventListeners.stream()
                    .filter(l -> l.notify(toDelete.getPluginFactory()))
                    .forEach(l -> l.unregister(toDelete.getInstanceId()));

            admin.deleteConfiguration(toDelete);
        }

        for (PluginConfiguration toUpdate : configsToBeUpdated) {
            admin.putConfiguration(toUpdate);
        }

    }

    private Map<ConfigurationId, PluginConfiguration> toMap(List<PluginInstanceReference<Object>> input) {
        Map<ConfigurationId, PluginConfiguration> res = new HashMap<>();
        for (PluginInstanceReference<?> p : input) {
            res.put(p.getConfiguration().getId(), p.getConfiguration());
        }
        return res;
    }
}
