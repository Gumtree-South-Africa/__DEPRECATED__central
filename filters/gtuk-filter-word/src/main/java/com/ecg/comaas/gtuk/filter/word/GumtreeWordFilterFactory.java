package com.ecg.comaas.gtuk.filter.word;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
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