package com.ecg.gumtree.comaas.filter.blacklist;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeBlacklistFilterFactory extends GumtreeFilterFactory<BlacklistFilterConfig, GumtreeBlacklistFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.blacklist.GumtreeBlacklistFilterConfiguration$BlacklistFilterFactory";

    public GumtreeBlacklistFilterFactory(GumshieldApi gumshieldApi) {
        super(BlacklistFilterConfig.class,
                (a, b) -> new GumtreeBlacklistFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withGumshieldApi(gumshieldApi));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
