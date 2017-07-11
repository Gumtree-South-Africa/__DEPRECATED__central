package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Provides sanity checks for the Riak cluster and also gives information about the overall sanity check health.
 *
 * @author mhuttar
 */
@Deprecated
class RiakClusterCheckProvider implements CheckProvider {
    private final ClusterModeManager clusterModeManager;

    private final Check majorityUpSanityCheck;

    @Autowired
    RiakClusterCheckProvider(ClusterModeManager clusterModeManager,
                             Check majorityUpSanityCheck) {
        this.clusterModeManager = clusterModeManager;
        this.majorityUpSanityCheck = majorityUpSanityCheck;
    }

    @Override
    public List<Check> getChecks() {
        List<Check> checks = newArrayList();
        checks.add(majorityUpSanityCheck);
        checks.add(new ClusterModeSanityCheck(clusterModeManager));

        return checks;
    }
}