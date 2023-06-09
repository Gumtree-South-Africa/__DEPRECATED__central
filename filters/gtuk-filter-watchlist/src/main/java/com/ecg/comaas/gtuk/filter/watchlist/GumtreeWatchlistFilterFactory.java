package com.ecg.comaas.gtuk.filter.watchlist;

import com.ecg.gumtree.comaas.common.filter.GumshieldClient;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
@Component
public class GumtreeWatchlistFilterFactory extends GumtreeFilterFactory<WatchlistFilterConfig, GumtreeWatchlistFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.watchlist.GumtreeWatchlistFilterConfiguration$WatchlistFilterFactory";

    @Autowired
    public GumtreeWatchlistFilterFactory(
            @Value("${gumshield.api.base_uri:localhost}") String apiBaseUri) {

        super(WatchlistFilterConfig.class,
                (a, b) -> new GumtreeWatchlistFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withChecklistApi(new GumshieldClient(apiBaseUri)));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}