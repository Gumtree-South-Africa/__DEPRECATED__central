package com.ecg.comaas.core.filter.ebayservices;

import com.ebay.ecg.gumtree.australia.lbs2.config.TrackingGeolocationConfiguration;
import com.ebay.ecg.gumtree.australia.lbs2.impl.TrackingGeoLocationServiceImpl;
import com.ecg.comaas.core.filter.ebayservices.ip2country.Ip2CountryFilterFactory;
import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.GtauLocationProvider;
import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.LocationProvider;
import com.ecg.comaas.core.filter.ebayservices.userstate.UserStateFilterFactory;
import com.ecg.comaas.core.filter.ebayservices.userstate.provider.UserStateBadgeBasedProvider;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import de.mobile.ebay.service.UserService;
import de.mobile.ebay.service.impl.ServiceConfigBean;
import de.mobile.ebay.service.impl.UserServiceImpl;
import de.mobile.ebay.service.impl.UserServiceStaticTokenProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Configuration
@Import(EbayServicesCommonConfiguration.class)
public class EbayServicesGtauConfiguration {

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.ip2country.enabled", havingValue = "true", matchIfMissing = true)
    public Ip2CountryFilterFactory ip2CountryFilterFactory(
            @Value("${comaas.filter.ebayservices.ip2country.geoV2.authUrl}") String authUrl,
            @Value("${comaas.filter.ebayservices.ip2country.geoV2.url}") String url,
            @Value("${comaas.filter.ebayservices.ip2country.geoV2.queryId}") String queryId,
            @Value("${comaas.filter.ebayservices.ip2country.geoV2.proxyHost:}") String proxyHost,
            @Value("${comaas.filter.ebayservices.ip2country.geoV2.proxyPort:}") Integer proxyPort) {
        TrackingGeolocationConfiguration geoLocationConfig = new TrackingGeolocationConfiguration(authUrl, url, queryId, proxyHost, proxyPort);
        LocationProvider locationProvider = new GtauLocationProvider(new TrackingGeoLocationServiceImpl(geoLocationConfig));
        return new Ip2CountryFilterFactory(locationProvider);
    }

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.userstate.enabled", havingValue = "true", matchIfMissing = true)
    public UserStateFilterFactory userStateFilterFactory(
            CloseableHttpClient httpClient,
            @Value("${comaas.filter.ebayservices.userstate.iafToken:}") String ebayServiceToken,
            @Value("${comaas.filter.ebayservices.userstate.endpointUrl}") String endpointUrl,
            @Value("${comaas.filter.ebayservices.userstate.appName:}") String appName,
            @Value("${comaas.filter.ebayservices.proxyUrl:}") String proxyUrl
    ) {
        ServiceConfigBean config = new ServiceConfigBean();
        config.setEndpointUrl(endpointUrl);
        config.setAppName(appName);
        config.setProxyUrl(proxyUrl);
        UserService userService = new UserServiceImpl(httpClient, config, new UserServiceStaticTokenProvider(ebayServiceToken));
        return new UserStateFilterFactory(new UserStateBadgeBasedProvider(userService));
    }
}
