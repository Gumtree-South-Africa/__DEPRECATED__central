package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.datastax.driver.core.Session;
import com.ecg.de.kleinanzeigen.replyts.volumefilter.registry.CassandraOccurrenceRegistry;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class VolumeFilterFactory implements FilterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilterFactory.class);

    private final EventStreamProcessor eventStreamProcessor;
    private final SharedBrain sharedBrain;
    private final Session session;
    private final Duration cassandraImplementationTimeout;
    private final boolean cassandraImplementationEnabled;
    private final int cassandraImplementationThreads;
    private final int maxAllowedRegisteredOccurrences;

    public VolumeFilterFactory(SharedBrain sharedBrain, EventStreamProcessor eventStreamProcessor, Session session,
                               Duration cassandraImplementationTimeout, boolean cassandraImplementationEnabled,
                               int maxAllowedRegisteredOccurrences, int cassandraImplementationThreads) {
        this.sharedBrain = sharedBrain;
        this.eventStreamProcessor = eventStreamProcessor;
        this.session = checkNotNull(session, "session");
        this.cassandraImplementationTimeout = checkNotNull(cassandraImplementationTimeout, cassandraImplementationTimeout);
        this.cassandraImplementationEnabled = cassandraImplementationEnabled;
        checkArgument(maxAllowedRegisteredOccurrences > 0, "maxAllowedRegisteredOccurrences must be strictly positive");
        this.maxAllowedRegisteredOccurrences = maxAllowedRegisteredOccurrences;
        checkArgument(cassandraImplementationThreads > 0, "cassandraImplementationThreads must be strictly positive");
        this.cassandraImplementationThreads = cassandraImplementationThreads;
    }

    @Nonnull
    @Override
    public Filter createPlugin(String instanceId, JsonNode jsonConfiguration) {
        ConfigurationParser configuration = new ConfigurationParser(jsonConfiguration);

        Set<Window> uniqueWindows = new HashSet<>();
        List<Quota> quotas = configuration.get();
        checkArgument(!quotas.isEmpty(), "the filter configuration must contain at least one quota");
        for (Quota quota : quotas) {
            Window window = new Window(instanceId, quota);
            if (uniqueWindows.contains(window)) {
                LOG.warn("window already exists '{}'", window.getWindowName());
            } else {
                uniqueWindows.add(window);
            }
        }

        if (cassandraImplementationEnabled) {
            checkMaximumPossibleOccurrenceCount(quotas, maxAllowedRegisteredOccurrences);
        }
        long longestQuotaPeriodMillis = getLongestQuotaPeriodMillis(quotas);

        LOG.info("Registering VolumeFilter with windows {}", uniqueWindows);
        eventStreamProcessor.register(uniqueWindows);

        return new VolumeFilter(sharedBrain, eventStreamProcessor, uniqueWindows, cassandraImplementationTimeout, cassandraImplementationEnabled,
                cassandraImplementationEnabled ? new CassandraOccurrenceRegistry(session, Duration.ofMillis(longestQuotaPeriodMillis)) : null,
                cassandraImplementationThreads);
    }

    static long getLongestQuotaPeriodMillis(List<Quota> quotas) {
        return quotas.stream()
                .mapToLong(Quota::getTimeSpanMillis)
                .max().orElseThrow(() -> new IllegalArgumentException("unable to find the longest quota"));
    }

    static void checkMaximumPossibleOccurrenceCount(Collection<Quota> quotas, int maxAllowedRegisteredOccurrences) {
        long shortestQuotaPeriodMillis = 0;
        long longestQuotaPeriodMillis = 1;
        int shortestQuotaMessageCountMax = 0;

        for (Quota quota : quotas) {
            long quotaPeriodMillis = quota.getTimeSpanMillis();
            if (quotaPeriodMillis > longestQuotaPeriodMillis) {
                longestQuotaPeriodMillis = quotaPeriodMillis;
            }
            if (quotaPeriodMillis <= shortestQuotaPeriodMillis || shortestQuotaPeriodMillis == 0) {
                shortestQuotaPeriodMillis = quotaPeriodMillis;
                if (quota.getAllowance() > shortestQuotaMessageCountMax) {
                    shortestQuotaMessageCountMax = quota.getAllowance();
                }
            }
        }

        long maxPossibleOccurrencesForThisInstance = (longestQuotaPeriodMillis / shortestQuotaPeriodMillis) * shortestQuotaMessageCountMax;
        if (maxPossibleOccurrencesForThisInstance > maxAllowedRegisteredOccurrences) {
            throw new IllegalArgumentException("Maximum allowed amount of occurrences to register is " + maxAllowedRegisteredOccurrences
                    + " but the provided configuration allows to register up to " + maxPossibleOccurrencesForThisInstance
                    + ". Either make the longest quota shorter or decrease allowance of the shortest quota.");
        }
    }
}
