package com.gumtree.comaas.filter.geoiplookup;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.comaas.common.filter.DisabledFilter;
import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.common.geoip.MaxMindGeoIpService;
import com.gumtree.filters.comaas.config.GeoIpLookupConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.json.ConfigMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.ZipFile;

@ComaasPlugin
@Configuration
public class GumtreeGeoIpLookupFilterConfiguration {
    @Bean
    GeoIpService geoIpService() {
        try {
            URL in = this.getClass().getClassLoader().getResource("GeoIPCountryCSV.zip");
            if (in == null) {
                throw new IllegalStateException("GeoIPCountryCSV.zip not found or not readable.");
            }
            ZipFile zipFile = new ZipFile(in.getFile());
            InputStream inputStream = zipFile.getInputStream(zipFile.entries().nextElement());
            return new MaxMindGeoIpService(new InputStreamReader(inputStream));
        } catch (IOException e) {
            throw new RuntimeException("Could not open GeoIP country CSV", e);
        }
    }

    @Bean
    public FilterFactory filterFactory(GeoIpService geoIpService) {
        return new GeoIpLookupFilterFactory(geoIpService);
    }

    public static class GeoIpLookupFilterFactory implements FilterFactory {
        private GeoIpService geoIpService;

        GeoIpLookupFilterFactory(GeoIpService geoIpService) {
            this.geoIpService = geoIpService;
        }

        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            String pluginFactory = configuration.get("pluginFactory").textValue();
            String instanceId = configuration.get("instanceId").textValue();
            JsonNode configurationNode = configuration.get("configuration");

            com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(pluginFactory, instanceId, configurationNode);
            GeoIpLookupConfig filterConfig = ConfigMapper.asObject(configurationNode.textValue(), GeoIpLookupConfig.class);

            if (filterConfig.getState() == State.DISABLED) {
                return new DisabledFilter(this.getClass());
            }

            return new GumtreeGeoIpLookupFilter().withPluginConfig(pluginConfig).withFilterConfig(filterConfig).withGeoIPService(geoIpService);
        }
    }
}
