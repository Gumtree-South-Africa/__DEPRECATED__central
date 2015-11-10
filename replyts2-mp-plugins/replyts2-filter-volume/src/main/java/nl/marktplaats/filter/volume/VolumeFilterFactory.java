package nl.marktplaats.filter.volume;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;
import nl.marktplaats.filter.volume.persistence.VolumeFilterEventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VolumeFilterFactory implements FilterFactory {

    private VolumeFilterEventRepository volumeFilterEventRepository;
    private VolumeFilterConfigParser volumeFilterConfigParser;

    public VolumeFilterFactory(VolumeFilterEventRepository volumeFilterEventRepository) {
        this(volumeFilterEventRepository, new VolumeFilterConfigParser());
    }

    public VolumeFilterFactory(VolumeFilterEventRepository volumeFilterEventRepository, VolumeFilterConfigParser volumeFilterConfigParser) {
        this.volumeFilterEventRepository = volumeFilterEventRepository;
        this.volumeFilterConfigParser = volumeFilterConfigParser;
    }

    public Filter createPlugin(String instanceName, JsonNode configurationAsJson) {
        VolumeFilterConfiguration config = volumeFilterConfigParser.parse(configurationAsJson);
        return new VolumeFilter(config, volumeFilterEventRepository);
    }


    static class VolumeFilterConfigParser {
        public VolumeFilterConfiguration parse(JsonNode configAsJson) {
            List<VolumeRule> rules = new ArrayList<>();
            configAsJson.elements().forEachRemaining(node -> {
                long timeSpan = node.get("timeSpan").asLong();
                TimeUnit timeUnit = TimeUnit.valueOf(node.get("timeUnit").asText().toUpperCase());
                long maxCount = node.get("maxCount").asLong();
                int score = node.get("score").asInt();
                rules.add(new VolumeRule(timeSpan, timeUnit, maxCount, score));
            });
            return new VolumeFilterConfiguration(rules);
        }
    }
}
