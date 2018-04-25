package com.ecg.comaas.gtuk.filter.blacklist;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Component
@Profile(TENANT_GTUK)
public class GumtreeBlacklistFilterFactory extends GumtreeFilterFactory<BlacklistFilterConfig, com.ecg.comaas.gtuk.filter.blacklist.GumtreeBlacklistFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.blacklist.GumtreeBlacklistFilterConfiguration$BlacklistFilterFactory";

    public GumtreeBlacklistFilterFactory(GumshieldApi gumshieldApi) {
        super(BlacklistFilterConfig.class,
                (a, b) -> new com.ecg.comaas.gtuk.filter.blacklist.GumtreeBlacklistFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withGumshieldApi(gumshieldApi));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
