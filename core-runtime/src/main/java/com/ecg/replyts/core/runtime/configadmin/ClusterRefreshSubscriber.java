package com.ecg.replyts.core.runtime.configadmin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Listens to Cluster Configuration refresh Events
 */
public class ClusterRefreshSubscriber {
    private final ITopic<Object> configChangeTopic;

    @Autowired
    ClusterRefreshSubscriber(HazelcastInstance h) {
        configChangeTopic = h.getTopic("config-change");
    }

    void attach(final Refresher refresher) {
        configChangeTopic.addMessageListener(new MessageListener<Object>() {
            @Override
            public void onMessage(Message<Object> objectMessage) {
                refresher.updateConfigurations();
            }
        });
    }
}
