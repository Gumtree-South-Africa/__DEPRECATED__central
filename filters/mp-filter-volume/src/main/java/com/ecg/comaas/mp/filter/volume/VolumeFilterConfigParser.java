package com.ecg.comaas.mp.filter.volume;

import com.ecg.comaas.mp.filter.volume.VolumeFilterConfiguration.VolumeRule;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Component
public class VolumeFilterConfigParser {
    public VolumeFilterConfiguration parse(JsonNode configAsJson) {
        List<VolumeRule> rules = StreamSupport.stream(configAsJson.spliterator(), false).map(this::createVolumeRule).collect(toList());
        return new VolumeFilterConfiguration(rules);
    }

    private VolumeRule createVolumeRule(JsonNode node) {
        long timeSpan = node.get("timeSpan").asLong();
        TimeUnit timeUnit = TimeUnit.valueOf(node.get("timeUnit").asText().toUpperCase());
        long maxCount = node.get("maxCount").asLong();
        int score = node.get("score").asInt();
        return new VolumeRule(timeSpan, timeUnit, maxCount, score);
    }
}