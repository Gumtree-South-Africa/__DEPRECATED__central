package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.hazelcast.config.MaxSizeConfig.MaxSizePolicy.USED_HEAP_PERCENTAGE;

class SharedBrain {

    private static final String VIOLATION_MEMORY_MAP_NAME = "violationMemoryMap";
    private static final int HAZELCAST_TIMEOUT_MS = 100;
    public static final int VIOLATION_MEMORY_MAX_HEAP_PERCENTAGE = 20;

    private final ITopic<String> communicationBus;
    private final AtomicReference<EventStreamProcessor> processor = new AtomicReference<>();
    private IMap<String, QuotaViolationRecord> violationMemoryMap; // email address => (score, description)

    SharedBrain(String name, HazelcastInstance hazelcastInstance, EventStreamProcessor eventStreamProcessor) {
        communicationBus = hazelcastInstance.getTopic("volumefilter_sender_address_exchange_" + name);
        processor.set(eventStreamProcessor);

        communicationBus.addMessageListener(message -> {
            Member publishingMember = message.getPublishingMember();
            if (publishingMember != null && !publishingMember.localMember()) {
                processor.get().mailReceivedFrom(message.getMessageObject());
            }
        });

        MapConfig violationMemoryMapConfig = new MapConfig(VIOLATION_MEMORY_MAP_NAME)
          .setEvictionPolicy(EvictionPolicy.LRU)
          .setMaxSizeConfig(new MaxSizeConfig(VIOLATION_MEMORY_MAX_HEAP_PERCENTAGE, USED_HEAP_PERCENTAGE));

        hazelcastInstance.getConfig().addMapConfig(violationMemoryMapConfig);

        violationMemoryMap = hazelcastInstance.getMap(VIOLATION_MEMORY_MAP_NAME);
    }

    public void markSeen(String mailAddress) {
        processor.get().mailReceivedFrom(mailAddress);
        communicationBus.publish(mailAddress);
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
