package com.ecg.replyts.integration;

import com.ecg.replyts.core.api.ClusterMonitor;

public class AlwaysHappyClusterMonitor implements ClusterMonitor {
    @Override
    public boolean allDatacentersAvailable() {
        return true;
    }

    @Override
    public String report() {
        return "I am a happy mock";
    }
}
