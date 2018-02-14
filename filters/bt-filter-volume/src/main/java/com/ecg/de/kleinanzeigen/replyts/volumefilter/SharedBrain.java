package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.hazelcast.config.MaxSizeConfig.MaxSizePolicy.USED_HEAP_PERCENTAGE;

@Component
public class SharedBrain {
    private static final String VIOLATION_MEMORY_MAP_NAME = "violationMemoryMap";

    private static final int HAZELCAST_TIMEOUT_MS = 100;
    private static final int VIOLATION_MEMORY_MAX_HEAP_PERCENTAGE = 20;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private IMap<String, QuotaViolationRecord> violationMemoryMap; // email address => (score, description)

    @PostConstruct
    public void addViolationConfig() {
        MapConfig violationMemoryMapConfig = new MapConfig(VIOLATION_MEMORY_MAP_NAME)
          .setEvictionPolicy(EvictionPolicy.LRU)
          .setMaxSizeConfig(new MaxSizeConfig(VIOLATION_MEMORY_MAX_HEAP_PERCENTAGE, USED_HEAP_PERCENTAGE));

        hazelcastInstance.getConfig().addMapConfig(violationMemoryMapConfig);

        violationMemoryMap = hazelcastInstance.getMap(VIOLATION_MEMORY_MAP_NAME);
    }

    public ITopic<String> createTopic(String name, EventStreamProcessor processor) {
        ITopic<String> topic = hazelcastInstance.getTopic("volumefilter_sender_address_exchange_" + name);

        topic.addMessageListener(message -> processor.mailReceivedFrom(message.getMessageObject()));

        return topic;
    }

    public void markSeen(ITopic<String> topic, String mailAddress) {
        topic.publish(mailAddress);
    }

    public void rememberViolation(String mailAddress, int score, String description, int ttlInSeconds) throws InterruptedException, ExecutionException, TimeoutException {
        violationMemoryMap.putAsync(
          mailAddress,
          new QuotaViolationRecord(score, description),
          ttlInSeconds,
          TimeUnit.SECONDS
        ).get(HAZELCAST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    public QuotaViolationRecord getViolationRecordFromMemory(String mailAddress) throws InterruptedException, ExecutionException, TimeoutException {
        return violationMemoryMap.getAsync(mailAddress).get(HAZELCAST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
}