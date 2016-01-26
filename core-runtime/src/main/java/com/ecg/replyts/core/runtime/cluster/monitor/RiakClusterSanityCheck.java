package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.ecg.replyts.core.runtime.SimpleSleeper;
import com.ecg.replyts.core.runtime.Sleeper;

import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.api.sanitychecks.Result.createResult;

public class RiakClusterSanityCheck implements Check {

    private final int maxTries;
    private final RiakClusterHealthCheck healthCheck;
    private final Sleeper sleeper;

    public RiakClusterSanityCheck(RiakClusterHealthCheck healthCheck, int maxTries) {
        this(healthCheck, maxTries, new SimpleSleeper());
    }

    // for test
    RiakClusterSanityCheck(RiakClusterHealthCheck healthCheck, int maxTries, Sleeper sleeper) {
        this.maxTries = maxTries;
        this.healthCheck = healthCheck;
        this.sleeper = sleeper;
    }

    /**
     * If the cluster is found to be non-healthy, depending on the {@code maxTries} settings retries will be done.
     * <b>Note:</b> This will have exponentially increasing sleeps!!!
     *
     * @return The result of the cluster check.
     */
    @Override
    public Result execute() throws InterruptedException {

        CheckResult checkResult = CheckResult.createNonHealthyEmpty();
        int tryCount = 0;

        while (tryCount < maxTries && !checkResult.isHealthy()) {
            if (tryCount > 0) {
                sleeper.sleep(TimeUnit.SECONDS, 1 << tryCount);
            }
            checkResult = healthCheck.check();
            tryCount++;
        }
        if (checkResult.isHealthy()) {
            return createResult(Status.OK, Message.detailed("More than half of all nodes available", checkResult.toString()));
        } else {
            return createResult(Status.CRITICAL, Message.detailed("There are less then half of all nodes in the cluster available!",
                    checkResult.toString()));
        }
    }

    @Override
    public String getName() {
        return "OVERALL";
    }

    @Override
    public String getCategory() {
        return "RIAK";
    }

    @Override
    public String getSubCategory() {
        return "ClusterNodeMajorityUpSanityCheck";
    }

}
