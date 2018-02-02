package com.ecg.replyts.core.runtime.configadmin;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class ClusterRefreshSubscriber {
    private final ITopic<Object> configChangeTopic;

    @Autowired
    private List<Refresher> refreshers;

    @Autowired
    public ClusterRefreshSubscriber(HazelcastInstance h) {
        configChangeTopic = h.getTopic("config-change");
    }

    @PostConstruct
    public void init() {
        refreshers.forEach(refresher -> configChangeTopic.addMessageListener(objectMessage -> refresher.updateConfigurations()));
    }
}