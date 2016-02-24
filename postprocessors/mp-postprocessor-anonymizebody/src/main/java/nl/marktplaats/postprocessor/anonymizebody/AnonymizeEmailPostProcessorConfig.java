package nl.marktplaats.postprocessor.anonymizebody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;
import java.util.stream.Collectors;

public class AnonymizeEmailPostProcessorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AnonymizeEmailPostProcessorConfig.class);

    private List<String> patterns = new ArrayList<>();

    @Autowired
    public AnonymizeEmailPostProcessorConfig(@Qualifier("replyts-properties") Properties properties) {
        this(properties
                .stringPropertyNames()
                .stream()
                .filter(key -> key.startsWith("anonymizebody.pattern."))
                .sorted((key1, key2) -> {
                        int position1 = Integer.parseInt(key1.substring("anonymizebody.pattern.".length()));
                        int position2 = Integer.parseInt(key2.substring("anonymizebody.pattern.".length()));
                        return position1 - position2;
                })
                .map(key -> properties.getProperty(key))
                .collect(Collectors.toList())
        );
    }

    public AnonymizeEmailPostProcessorConfig(List<String> patterns) {
        this.patterns = patterns;
        patterns.forEach(pattern -> LOG.info("Cleaning outgoing emails with matches of: /{}/", pattern));
    }

    public List<String> getPatterns() {
        return patterns;
    }
}
