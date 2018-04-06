package com.ecg.gumtree.comaas.filter.word;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeWordFilterFactory extends GumtreeFilterFactory<WordFilterConfig, GumtreeWordFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.word.GumtreeWordFilterConfiguration$WordFilterFactory";

    public GumtreeWordFilterFactory() {
        super(WordFilterConfig.class, GumtreeWordFilter::new);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}