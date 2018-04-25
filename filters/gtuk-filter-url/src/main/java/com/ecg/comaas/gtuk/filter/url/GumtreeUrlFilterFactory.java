package com.ecg.comaas.gtuk.filter.url;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.UrlFilterConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Component
public class GumtreeUrlFilterFactory extends GumtreeFilterFactory<UrlFilterConfig, GumtreeUrlFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.url.GumtreeUrlFilterConfiguration$UrlFilterFactory";

    public GumtreeUrlFilterFactory() {
        super(UrlFilterConfig.class, (a, b) -> new GumtreeUrlFilter().withPluginConfig(a).withFilterConfig(b));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}