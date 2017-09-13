package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

/**
 * Regularly checks for modifications of plugin configurations and informs the responsible {@link ConfigurationAdmin}
 * about this change.
 */
public class Refresher {
    private static final Logger LOG = LoggerFactory.getLogger(Refresher.class);

    private static final Duration REFRESH_RATE = Duration.ofMinutes(1);
    private final String threadName;

    @Autowired
    private ConfigurationRepository repository;

    @Autowired(required = false)
    private List<ConfigurationRefreshEventListener> refreshEventListeners = Collections.emptyList();

    private ConfigurationAdmin<Object> admin;

    private final ScheduledExecutorService refreshScheduler;

    public Refresher(ConfigurationAdmin<Object> admin) {
        this.admin = admin;
        this.threadName = "configuration-checker-" + admin.getAdminName();
        this.refreshScheduler = newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadName).build());
    }

    @PostConstruct
    public void initialize() {
        LOG.info("Refreshing configurations now and periodically refreshing with daemon thread {} every {}", threadName,
                formatDurationWords(REFRESH_RATE.toMillis(), true, true));

        updateConfigurations();

        refreshScheduler.scheduleWithFixedDelay(setTaskFields(this::refreshConfiguration, threadName),
                REFRESH_RATE.toNanos(), REFRESH_RATE.toNanos(), NANOSECONDS);
    }

    private void refreshConfiguration() {
        try {
            updateConfigurations();
        } catch (RuntimeException e) {
            LOG.error("Updating Plugin Configurations from Database failed", e);
        }
    }

    @PreDestroy
    public void destroy() {
        refreshScheduler.shutdown();
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
                if (listener.isApplicable(toDelete.findFactoryClass())) {
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