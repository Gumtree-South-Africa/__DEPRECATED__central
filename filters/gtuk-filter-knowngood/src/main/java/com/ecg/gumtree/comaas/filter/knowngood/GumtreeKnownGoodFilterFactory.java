package com.ecg.gumtree.comaas.filter.knowngood;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeKnownGoodFilterFactory extends GumtreeFilterFactory<KnownGoodFilterConfig, GumtreeKnownGoodFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.knowngood.GumtreeKnownGoodFilterConfiguration$KnownGoodFilterFactory";

    public GumtreeKnownGoodFilterFactory(GumshieldApi gumshieldApi) {
        super(KnownGoodFilterConfig.class,
                (a, b) -> new GumtreeKnownGoodFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withUserApi(gumshieldApi.userApi()));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}