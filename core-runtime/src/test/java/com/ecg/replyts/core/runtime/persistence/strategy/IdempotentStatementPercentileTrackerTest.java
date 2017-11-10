package com.ecg.replyts.core.runtime.persistence.strategy;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdempotentStatementPercentileTrackerTest {

    private static final int HIGHEST_TRACKABLE_LATENCY_MILLIS = 10;

    private QueryOptions queryOptions;
    private IdempotentStatementPercentileTracker tracker;
    private Host host;

    @Before
    public void setUp() {
        tracker = IdempotentStatementPercentileTracker
                .builder(HIGHEST_TRACKABLE_LATENCY_MILLIS)
                .setIntervalMs(0)
                .setMinRecordedValues(1)
                .build();

        host = mock(Host.class);
        queryOptions = new QueryOptions();

        Configuration configuration = mock(Configuration.class);
        when(configuration.getQueryOptions()).thenReturn(queryOptions);

        Cluster cluster = mock(Cluster.class);
        when(cluster.getConfiguration()).thenReturn(configuration);
        tracker.onRegister(cluster);
    }

    @Test
    public void trackAllIdempotantStatement() {
        queryOptions.setDefaultIdempotence(false);  //To be sure that default idempotence doesn't have impact

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }

    private SimpleStatement simpleStatementWithIdempotance(Boolean idempotent) {
        SimpleStatement statement = new SimpleStatement(null);
        if (idempotent != null) {
            statement.setIdempotent(idempotent);
        }
        return statement;
    }

    private long getLatencyAt50thPercentile(IdempotentStatementPercentileTracker tracker) {
        //50th percentile (the middle of tracked statement set)
        return tracker.getLatencyAtPercentile(null, null, null, 50);
    }

    @Test
    public void ignoreNonIdempotantStatements() {
        queryOptions.setDefaultIdempotence(true);  //To be sure that default idempotence doesn't have impact

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, simpleStatementWithIdempotance(false), null, TimeUnit.MILLISECONDS.toNanos(4));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }

    @Test
    public void trackUnknownIdempotantStatementWhenIdempotentByDefault() {
        queryOptions.setDefaultIdempotence(true);

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(null), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, simpleStatementWithIdempotance(null), null, TimeUnit.MILLISECONDS.toNanos(4));
        tracker.update(host, simpleStatementWithIdempotance(null), null, TimeUnit.MILLISECONDS.toNanos(5));

        assertEquals(3, getLatencyAt50thPercentile(tracker));
    }

    @Test
    public void ignoreUnknownIdempotantStatementWhenNotIdempotentByDefault() {
        queryOptions.setDefaultIdempotence(false);

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, simpleStatementWithIdempotance(null), null, TimeUnit.MILLISECONDS.toNanos(4));
        tracker.update(host, simpleStatementWithIdempotance(null), null, TimeUnit.MILLISECONDS.toNanos(5));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }

    @Test
    public void ignoreBatchStatement() {
        queryOptions.setDefaultIdempotence(true);  //To be sure that default idempotence doesn't have impact

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, new BatchStatement(), null, TimeUnit.MILLISECONDS.toNanos(4));
        tracker.update(host, new BatchStatement(), null, TimeUnit.MILLISECONDS.toNanos(5));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }

    @Test
    public void ignoreStatementWithLatancyHigherThanHighestTrackable() {
        queryOptions.setDefaultIdempotence(true);  //To be sure that default idempotence doesn't have impact

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(
                HIGHEST_TRACKABLE_LATENCY_MILLIS + 1));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }

    @Test
    public void ignoreIdempotantStatementsWitExcludableException() {
        queryOptions.setDefaultIdempotence(false);

        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(1));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(2));
        tracker.update(host, simpleStatementWithIdempotance(true), null, TimeUnit.MILLISECONDS.toNanos(3));
        tracker.update(host, simpleStatementWithIdempotance(true), new InvalidQueryException(null), TimeUnit.MILLISECONDS.toNanos(4));
        tracker.update(host, simpleStatementWithIdempotance(true), new InvalidQueryException(null), TimeUnit.MILLISECONDS.toNanos(5));

        assertEquals(2, getLatencyAt50thPercentile(tracker));
    }
}
