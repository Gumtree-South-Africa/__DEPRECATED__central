package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.ecg.replyts.core.runtime.cluster.ClusterMode;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;

@Deprecated
class ClusterModeSanityCheck implements Check {
    private final ClusterModeManager clusterModeManager;

    ClusterModeSanityCheck(ClusterModeManager clusterModeManager) {
        this.clusterModeManager = clusterModeManager;
    }

    @Override
    public Result execute() throws Exception {
        ClusterMode clusterMode = clusterModeManager.determineMode();
        switch (clusterMode) {
            case OK:
                return Result.createResult(Status.OK, Message.shortInfo("Cluster Mode is OK"));
            case FAILOVER:
                return Result.createResult(Status.WARNING, Message.shortInfo("In Failover mode (Continuing Operations)"));
            case BLOCKED:
                return Result.createResult(Status.CRITICAL, Message.shortInfo("In Blocked mode (stopping all operations. Check logs for details)"));
            default:
                return Result.createResult(Status.WARNING, Message.shortInfo("Cluster mode is " + clusterMode));
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
        return "ClusterMode";
    }
}
