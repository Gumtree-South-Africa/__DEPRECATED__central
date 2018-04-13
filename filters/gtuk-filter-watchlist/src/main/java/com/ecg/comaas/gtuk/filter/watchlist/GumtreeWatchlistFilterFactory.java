package com.ecg.comaas.gtuk.filter.watchlist;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.filters.comaas.config.WatchlistFilterConfig;
import com.gumtree.gumshield.api.client.GumshieldApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeWatchlistFilterFactory extends GumtreeFilterFactory<WatchlistFilterConfig, GumtreeWatchlistFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.watchlist.GumtreeWatchlistFilterConfiguration$WatchlistFilterFactory";

    @Autowired
    public GumtreeWatchlistFilterFactory(GumshieldApi gumshieldApi) {
        super(WatchlistFilterConfig.class,
                (a, b) -> new GumtreeWatchlistFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withChecklistApi(gumshieldApi.checklistApi()));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}