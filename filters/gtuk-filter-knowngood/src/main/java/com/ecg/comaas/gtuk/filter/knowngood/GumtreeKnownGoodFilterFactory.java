package com.ecg.comaas.gtuk.filter.knowngood;

import com.ecg.gumtree.comaas.common.filter.GumshieldClient;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.KnownGoodFilterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Component
public class GumtreeKnownGoodFilterFactory extends GumtreeFilterFactory<KnownGoodFilterConfig, GumtreeKnownGoodFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.knowngood.GumtreeKnownGoodFilterConfiguration$KnownGoodFilterFactory";

    public GumtreeKnownGoodFilterFactory(
            @Value("${gumshield.api.base_uri:localhost}") String apiBaseUri) {

        super(KnownGoodFilterConfig.class,
                (a, b) -> new GumtreeKnownGoodFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withUserApi(new GumshieldClient(apiBaseUri)));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}