package com.ecg.replyts.core.runtime.persistence.strategy;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PercentileTracker;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.BootstrappingException;
import com.datastax.driver.core.exceptions.OverloadedException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import com.datastax.driver.core.exceptions.UnavailableException;
import com.datastax.driver.core.exceptions.UnpreparedException;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A {@link PercentileTracker} implementation that records query latencies over a sliding time interval based on <a href="https://en.wikipedia.org/wiki/Idempotence">idempotence</a>,
 * and exposes an API to retrieve the latency at a given percentile.
 * <p/>
 * The method {@link #include(Host, Statement, Exception)} filter out {@link BatchStatement} and non-idempotent statements (see {@link com.datastax.driver.core.Statement#isIdempotent()})
 * <p/>
 * If idempotence is unknown (null) - default idempotence is being used (see {@link QueryOptions#getDefaultIdempotence()})
 * <p/>
 * <b>Note:</b> the class can be eliminated as soon as an <a href="https://datastax-oss.atlassian.net/browse/JAVA-1667">issue</a> is done
 * <p/>
 * <b>Note:</b> {@link com.ecg.replyts.core.runtime.persistence.strategy.IdempotentStatementPercentileTracker#includeException(java.lang.Exception)} method is work around cause of a bug in DataStax Java Driver
 * (see <a href="https://datastax-oss.atlassian.net/browse/JAVA-1667">DataStax Java Driver Jira ticker</a> for more details)
 */
public class IdempotentStatementPercentileTracker extends PercentileTracker {

    private volatile Cluster cluster;

    private IdempotentStatementPercentileTracker(long highestTrackableLatencyMillis, int numberOfSignificantValueDigits, int minRecordedValues, long intervalMs) {
        super(highestTrackableLatencyMillis, numberOfSignificantValueDigits, minRecordedValues, intervalMs);
    }

    @Override
    public void onRegister(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    protected Cluster computeKey(Host host, Statement statement, Exception exception) {
        return cluster;
    }

    @Override
    protected boolean include(Host host, Statement statement, Exception exception) {
        return !(statement instanceof BatchStatement) // BatchStatement is filtered out, cause it can contain any number of statements (both idempotent and non-idempotent)
                && isIdempotent(statement)
                && includeException(exception);
    }

    /**
     * Is the statement is idempotent
     * @param statement statement
     * @return statement.isIdempotent() if not null, otherwise default idempotence from query options
     * @see QueryOptions#getDefaultIdempotence()
     */
    private boolean isIdempotent(Statement statement) {
        return statement.isIdempotent() != null
                    ? statement.isIdempotent()
                    : cluster.getConfiguration().getQueryOptions().getDefaultIdempotence();
    }

    /**
     * Non-null exception is going to be included only if it's not an instance of one of excluded exceptions
     */
    private boolean includeException(Exception exception) {
        if (exception == null) {
            return true;
        }

        for (Class<? extends Exception> excludedException : EXCLUDED_EXCEPTIONS) {
            if (excludedException.isInstance(exception)) {
                return false;
            }
        }

        return true;
    }

    /**
     * A set of DriverException subclasses that we should prevent from updating the host's score.
     * The intent behind it is to filter out "fast" errors: when a host replies with such errors,
     * it usually does so very quickly, because it did not involve any actual
     * coordination work. Such errors are not good indicators of the host's responsiveness,
     * and tend to make the host's score look better than it actually is.
     */
    private static final Set<Class<? extends Exception>> EXCLUDED_EXCEPTIONS = ImmutableSet.of(
            UnavailableException.class, // this is done via the snitch and is usually very fast
            OverloadedException.class,
            BootstrappingException.class,
            UnpreparedException.class,
            QueryValidationException.class // query validation also happens at early stages in the coordinator
    );

    /**
     * Returns a builder to create a new instance.
     *
     * @param highestTrackableLatencyMillis the highest expected latency. If a higher value is reported, it will be
     *                                      ignored and a warning will be logged. A good rule of thumb is to set it
     *                                      slightly higher than {@link SocketOptions#getReadTimeoutMillis()}.
     * @return the builder.
     */
    public static IdempotentStatementPercentileTracker.Builder builder(long highestTrackableLatencyMillis) {
        return new IdempotentStatementPercentileTracker.Builder(highestTrackableLatencyMillis);
    }

    /**
     * Helper class to build {@code IdempotentStatementPercentileTracker} instances with a fluent interface.
     */
    public static class Builder {

        private long highestTrackableLatencyMillis;
        private int numberOfSignificantValueDigits = 3;
        private int minRecordedValues = 1000;
        private long intervalMs = MINUTES.toMillis(5);

        Builder(long highestTrackableLatencyMillis) {
            this.highestTrackableLatencyMillis = highestTrackableLatencyMillis;
        }

        public IdempotentStatementPercentileTracker build() {
            return new IdempotentStatementPercentileTracker(highestTrackableLatencyMillis, numberOfSignificantValueDigits,
                    minRecordedValues, intervalMs);
        }

        public Builder setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
            return this;
        }

        public Builder setMinRecordedValues(int minRecordedValues) {
            this.minRecordedValues = minRecordedValues;
            return this;
        }
    }
}
