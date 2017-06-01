package com.ecg.gumtree.comaas.filter.geoip;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.filters.comaas.config.GeoIpFilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(GumtreeGeoIpFilterConfiguration.GeoIpFilterFactory.class)
public class GumtreeGeoIpFilterConfiguration {
    @Bean
    public FilterFactory filterFactory() {
        return new GeoIpFilterFactory();
    }

    static class GeoIpFilterFactory extends GumtreeFilterFactory<GeoIpFilterConfig, GumtreeGeoIpFilter> {
        GeoIpFilterFactory() {
            super(GeoIpFilterConfig.class, (a, b) -> new GumtreeGeoIpFilter().withPluginConfig(a).withFilterConfig(b));
        }
    }
}
