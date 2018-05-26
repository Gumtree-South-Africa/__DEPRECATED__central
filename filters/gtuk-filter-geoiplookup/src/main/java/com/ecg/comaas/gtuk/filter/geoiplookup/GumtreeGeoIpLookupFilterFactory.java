package com.ecg.comaas.gtuk.filter.geoiplookup;

import com.ecg.gumtree.comaas.common.domain.GeoIpLookupFilterConfig;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Profile(TENANT_GTUK)
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
