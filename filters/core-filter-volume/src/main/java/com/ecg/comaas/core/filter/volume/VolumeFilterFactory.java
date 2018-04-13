package com.ecg.comaas.core.filter.volume;

import com.datastax.driver.core.Session;
import com.ecg.comaas.core.filter.volume.registry.CassandraOccurrenceRegistry;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@ComaasPlugin
@Component
public class VolumeFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.volumefilter.VolumeFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilterFactory.class);

    private final Session session;
    private final int maxAllowedRegisteredOccurrences;

    public VolumeFilterFactory(
            @Qualifier("cassandraSessionForCore") Session session,
            @Value("${filter.volume.max.occurrences.allowed:10000}") int maxAllowedRegisteredOccurrences) {
        this.session = checkNotNull(session, "session");
        checkArgument(maxAllowedRegisteredOccurrences > 0, "maxAllowedRegisteredOccurrences must be strictly positive");
        this.maxAllowedRegisteredOccurrences = maxAllowedRegisteredOccurrences;
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

        checkMaximumPossibleOccurrenceCount(quotas, maxAllowedRegisteredOccurrences);
        long longestQuotaPeriodMillis = getLongestQuotaPeriodMillis(quotas);

        return new VolumeFilter(uniqueWindows, new CassandraOccurrenceRegistry(session, Duration.ofMillis(longestQuotaPeriodMillis)));
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

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
