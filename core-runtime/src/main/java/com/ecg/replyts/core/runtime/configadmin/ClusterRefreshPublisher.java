package com.ecg.replyts.core.runtime.configadmin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Async Event bus that is used by the config admin to inform other replyts nodes in the cluster that the filter config
 * was just changed and therefore needs to be resynced with database.
 */
public class ClusterRefreshPublisher {


    private final ITopic<Object> configChangeTopic;

    @Autowired
    ClusterRefreshPublisher(HazelcastInstance h) {
        configChangeTopic = h.getTopic("config-change");
    }

    void publish() {
        configChangeTopic.publish(System.currentTimeMillis());
    }

}
