package com.ecg.replyts.core.runtime.cluster.monitor;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;

final class CheckResult {

    private final boolean healthy;
    private final Set<String> healthyNodes;
    private final Set<String> impairedNodes;

    static CheckResult createNonHealthyEmpty() {
        return new CheckResult(false, emptySet(), emptySet());
    }

    static CheckResult createHealthyEmpty() {
        return new CheckResult(true, emptySet(), emptySet());
    }

    CheckResult(boolean healthy, Set<String> healthyNodes, Set<String> impairedNodes) {
        this.healthy = healthy;
        this.healthyNodes = ImmutableSet.copyOf(healthyNodes);
        this.impairedNodes = ImmutableSet.copyOf(impairedNodes);
    }

    boolean isHealthy() {
        return healthy;
    }

    Set<String> getHealthyNodes() {
        return healthyNodes;
    }

    Set<String> getImpairedNodes() {
        return impairedNodes;
    }

    @Override
    public String toString() {
        return format("Cluster healthy: %s; healthy nodes: %s; impaired nodes: %s",
                healthy, Joiner.on(",").join(healthyNodes), Joiner.on(",").join(impairedNodes));
    }
}
