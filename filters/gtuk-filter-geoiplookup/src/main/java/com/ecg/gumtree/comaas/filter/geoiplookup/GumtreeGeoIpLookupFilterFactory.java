package com.ecg.gumtree.comaas.filter.geoiplookup;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.filters.comaas.config.GeoIpLookupFilterConfig;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
@Import(GumtreeGeoIpLookupConfiguration.class)
public class GumtreeGeoIpLookupFilterFactory extends GumtreeFilterFactory<GeoIpLookupFilterConfig, GumtreeGeoIpLookupFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.geoiplookup.GumtreeGeoIpLookupFilterConfiguration$GeoIpLookupFilterFactory";

    public GumtreeGeoIpLookupFilterFactory(GeoIpService geoIpService) {
        super(GeoIpLookupFilterConfig.class,
                (a, b) -> new GumtreeGeoIpLookupFilter()
                        .withPluginConfig(a)
                        .withFilterConfig(b)
                        .withGeoIPService(geoIpService));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
