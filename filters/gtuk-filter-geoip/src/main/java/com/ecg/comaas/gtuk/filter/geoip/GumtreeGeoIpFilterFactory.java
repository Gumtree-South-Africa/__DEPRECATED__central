package com.ecg.comaas.gtuk.filter.geoip;

import com.ecg.gumtree.comaas.common.domain.GeoIpFilterConfig;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
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