package com.ecg.de.kleinanzeigen.replyts.volumefilter.registry;

import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraOccurrenceRegistryIntegrationTest {
    private static final Date NOW = new Date();
    private static final String USER = "jdoe@example.com";

    @Test
    public void differentMessageIds() throws Exception {
        try (VolumeFilterSchema schema = VolumeFilterSchema.create()) {
            OccurrenceRegistry registry = new CassandraOccurrenceRegistry(schema.session, Duration.ofMinutes(1));
            IntStream.range(0, 5).mapToObj(String::valueOf).forEach(i -> registry.register(USER, i, NOW));
            assertEquals(5, registry.count(USER, Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
        }
    }

    @Test
    public void sameMessageIdOccurredMultipleTimes() throws Exception {
        try (VolumeFilterSchema schema = VolumeFilterSchema.create()) {
            OccurrenceRegistry registry = new CassandraOccurrenceRegistry(schema.session, Duration.ofMinutes(1));
            IntStream.range(0, 5).mapToObj(String::valueOf).forEach(i -> registry.register(USER, "whatever", NOW));
            assertEquals(1, registry.count(USER, Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
        }
    }

    @Test
    public void expiration() throws Exception {
        try (VolumeFilterSchema schema = VolumeFilterSchema.create()) {
            OccurrenceRegistry registry = new CassandraOccurrenceRegistry(schema.session, Duration.ofSeconds(1));
            IntStream.range(0, 5).mapToObj(String::valueOf).forEach(i -> registry.register(USER, i, NOW));
            assertTrue(registry.count(USER, Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))) > 0);
            Thread.sleep(1_000);
            assertEquals(0, registry.count(USER, Date.from(Instant.now().minus(1, ChronoUnit.MINUTES))));
        }
    }

    @Test
    public void properCount() throws Exception {
        try (VolumeFilterSchema schema = VolumeFilterSchema.create()) {
            OccurrenceRegistry registry = new CassandraOccurrenceRegistry(schema.session, Duration.ofMinutes(1));
            IntStream.range(0, 5).forEach(i -> registry.register(USER, String.valueOf(i), Date.from(NOW.toInstant().minus(i, ChronoUnit.MINUTES))));
            assertEquals(2, registry.count(USER, Date.from(NOW.toInstant().minus(2, ChronoUnit.MINUTES))));
        }
    }

    private static class VolumeFilterSchema implements AutoCloseable {
        private final String keyspace;
        private final Session session;

        VolumeFilterSchema() {
            this.keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
            this.session = CassandraIntegrationTestProvisioner.getInstance().loadSchema(keyspace, "core_volume_filter.cql");
        }

        @Override
        public void close() throws Exception {
            CassandraIntegrationTestProvisioner.getInstance().cleanTables(session, keyspace);
        }

        static VolumeFilterSchema create() {
            return new VolumeFilterSchema();
        }
    }
}
