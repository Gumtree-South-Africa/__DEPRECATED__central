package com.ecg.comaas.gtuk.filter.flagged;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.UserFlaggedFilterConfig;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeUserFlaggedFilterFactory extends GumtreeFilterFactory<UserFlaggedFilterConfig, GumtreeUserFlaggedFilter> {

    private static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.flagged.GumtreeUserFlaggedFilterConfiguration$UserFlaggedFilterFactory";

    public GumtreeUserFlaggedFilterFactory() {
        super(com.gumtree.filters.comaas.config.UserFlaggedFilterConfig.class,
                (a, b) -> new GumtreeUserFlaggedFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}