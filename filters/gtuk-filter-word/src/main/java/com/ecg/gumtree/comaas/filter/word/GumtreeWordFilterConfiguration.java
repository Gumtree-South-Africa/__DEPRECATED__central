package com.ecg.gumtree.comaas.filter.word;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeWordFilterConfiguration.WordFilterFactory.class)
public class GumtreeWordFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new WordFilterFactory();
    }

    static class WordFilterFactory extends GumtreeFilterFactory<WordFilterConfig, GumtreeWordFilter> {
        WordFilterFactory() {
            super(WordFilterConfig.class, GumtreeWordFilter::new);
        }
    }
}
