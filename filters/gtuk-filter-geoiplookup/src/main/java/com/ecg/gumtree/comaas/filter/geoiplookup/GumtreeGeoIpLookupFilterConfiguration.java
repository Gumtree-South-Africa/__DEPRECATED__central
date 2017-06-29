package com.ecg.gumtree.comaas.filter.geoiplookup;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.common.geoip.MaxMindGeoIpService;
import com.gumtree.filters.comaas.config.GeoIpLookupFilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

@ComaasPlugin
@Configuration
@Import(GumtreeGeoIpLookupFilterConfiguration.GeoIpLookupFilterFactory.class)
public class GumtreeGeoIpLookupFilterConfiguration {
    @Bean
    GeoIpService geoIpService() {
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("GeoIPCountryCSV.zip");
            if (in == null) {
                throw new IllegalStateException("GeoIPCountryCSV.zip not found or not readable.");
            }
            ZipInputStream zipInputStream = new ZipInputStream(in);
            zipInputStream.getNextEntry();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zipInputStream, "UTF-8"));
            return new MaxMindGeoIpService(bufferedReader);
        } catch (IOException e) {
            throw new RuntimeException("Could not open GeoIP country CSV", e);
        }
    }

    @Bean
    public FilterFactory filterFactory(GeoIpService geoIpService) {
        return new GeoIpLookupFilterFactory(geoIpService);
    }

    static class GeoIpLookupFilterFactory extends GumtreeFilterFactory<GeoIpLookupFilterConfig, GumtreeGeoIpLookupFilter> {
        GeoIpLookupFilterFactory(GeoIpService geoIpService) {
            super(GeoIpLookupFilterConfig.class,
                    (a, b) -> new GumtreeGeoIpLookupFilter()
                            .withPluginConfig(a)
                            .withFilterConfig(b)
                            .withGeoIPService(geoIpService));
        }
    }
}
