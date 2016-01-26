package com.ecg.replyts.core.runtime.cluster;

import com.codahale.metrics.Gauge;
import com.ecg.replyts.core.api.ClusterMonitor;
import com.ecg.replyts.core.runtime.ExpiringReference;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks the cluster's health and determines the Operational state of ReplyTS (for case of split brain scenarios).
 * <p/>
 * If Riak is operated in multiple datacenters, this {@link ClusterModeManager} will ensure that there is no net split.
 * As ReplyTS may not operate in all datacenters in case of a split brain, it will stop operation in all datacenters ( {@link ClusterMode#BLOCKED} )
 * until one datacenter is set to {@link ClusterMode#FAILOVER}. After Recovery ReplyTS will switch back to {@link ClusterMode#BLOCKED} (or stay in there) to
 * give riak some time to replicate and ops the chance to see if things are back to normal.
 * <br/>
 * Once sure that everything's back to normal, normal mode can be enabled via {@link #switchToNormal()}.
 *
 * @author smoczarski
 * @see <a href="https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two-datacenter-operations">Wiki Article: 2 Datacenter Operations</a>
 */
public class ClusterModeManager {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterModeManager.class);

    private final ClusterMonitor monitor;

    private final AtomicReference<ClusterMode> mode = new AtomicReference<>(ClusterMode.OK);

    private final ExpiringReference<Boolean> blockedWarningWasLoggedInTheLastMinute = ExpiringReference.<Boolean>validFor(1, TimeUnit.MINUTES).afterwards(Boolean.FALSE);

    ClusterModeManager(ClusterMonitor monitor) {
        this.monitor = monitor;
        TimingReports.newGauge("normal-operations-mode", new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return mode.get() == ClusterMode.OK ? 1 : 0;
            }
        });
    }

    public ClusterMode determineMode() {

        boolean allDatacentersAvailable = monitor.allDatacentersAvailable();
        boolean inNormalOperationMode = mode.get() == ClusterMode.OK;

        if (inNormalOperationMode ^ allDatacentersAvailable) {

            mode.set(ClusterMode.BLOCKED);
            logModeChangeToBlocked();
        }
        return mode.get();
    }

    private void logModeChangeToBlocked() {
        if (!blockedWarningWasLoggedInTheLastMinute.get()) {

            LOG.error("SWITCHING TO BLOCKED MODE! \n" +
                    "This is either because a Split Brain was detected or a Split Brain Recovery was detected.\n" +
                    "Both events put Riak into an inconsistent state and need Manual interaction.\n" +
                    "More infos can be found at: \n" +
                    "  - The Sanity Checks (which hosts and which datacenter is not available)\n" +
                    "  - Documentation: https://github.scm.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two%20Datacenter%20Operations");
            LOG.error("Results: {}", monitor.report());

            blockedWarningWasLoggedInTheLastMinute.set(Boolean.TRUE);
        }
    }


    void switchToFailover() {
        Preconditions.checkState(
                mode.compareAndSet(ClusterMode.BLOCKED, ClusterMode.FAILOVER),
                "Can not enable FAILOVER mode, because currently not in BLOCKED mode");

        LOG.info("ENABLING FAILOVER MODE: ReplyTS will continue processing mails (only do this in one datacenter!). " +
                "Note that this will not fix your problem permanently. ");
    }

    void switchToNormal() {
        Preconditions.checkState(
                monitor.allDatacentersAvailable(),
                "Can not enable NORMAL mode, because Riak Cluster is not available");

        Preconditions.checkState(
                mode.compareAndSet(ClusterMode.BLOCKED, ClusterMode.OK),
                "Can not enable NORMAL mode, because currently not in BLOCKED mode.");

        LOG.info("REENABLING NORMAL OPERATIONS: ReplyTS will continue processing mails. Everything fine again. ");
    }

}
