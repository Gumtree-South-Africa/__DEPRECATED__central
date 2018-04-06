package com.ecg.gumtree.comaas.filter.geoip;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class GumtreeGeoIpFilterFactory extends GumtreeFilterFactory<GeoIpFilterConfig, GumtreeGeoIpFilter> {

    public static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.geoip.GumtreeGeoIpFilterConfiguration$GeoIpFilterFactory";

    public GumtreeGeoIpFilterFactory() {
        super(GeoIpFilterConfig.class, (a, b) -> new GumtreeGeoIpFilter().withPluginConfig(a).withFilterConfig(b));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}