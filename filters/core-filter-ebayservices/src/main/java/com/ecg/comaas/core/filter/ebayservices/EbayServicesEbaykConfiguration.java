package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.comaas.core.filter.ebayservices.config.UserStateServiceConfig;
import com.ecg.comaas.core.filter.ebayservices.ip2country.Ip2CountryFilterFactory;
import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.EbaykLocationProvider;
import com.ecg.comaas.core.filter.ebayservices.ip2country.provider.LocationProvider;
import com.ecg.comaas.core.filter.ebayservices.userstate.UserStateFilterFactory;
import com.ecg.comaas.core.filter.ebayservices.userstate.provider.UserStateProfileBasedProvider;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import de.mobile.ebay.service.UserProfileService;
import de.mobile.ebay.service.lbs.TrackingGeoLocationServiceImpl;
import de.mobile.ebay.service.restclient.RestClientFactory;
import de.mobile.ebay.service.userprofile.UserProfileServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Configuration
@Import(EbayServicesCommonConfiguration.class)
public class EbayServicesEbaykConfiguration {

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.ip2country.enabled", havingValue = "true", matchIfMissing = true)
    public Ip2CountryFilterFactory ip2CountryFilterFactory(
            RestClientFactory restClientFactory,
            @Value("${comaas.filter.ebayservices.ip2country.geoV1.endpointUrl}") String endpointUrl,
            @Value("${comaas.filter.ebayservices.ip2country.geoV1.clientId}") String clientId,
            @Value("${comaas.filter.ebayservices.ip2country.geoV1.clientSecret}") String clientSecret,
            @Value("${comaas.filter.ebayservices.ip2country.geoV1.oauthUrl}") String oAuthUrl,
            @Value("${comaas.filter.ebayservices.ip2country.geoV1.queryId}") String queryId
    ) {
        de.mobile.ebay.service.lbs.ServiceConfigBean serviceConfigBean = new de.mobile.ebay.service.lbs.ServiceConfigBean();
        serviceConfigBean.setEndpointUrl(endpointUrl);
        serviceConfigBean.setClientId(clientId);
        serviceConfigBean.setClientSecret(clientSecret);
        serviceConfigBean.setOAuthUrl(oAuthUrl);
        serviceConfigBean.setQueryId(queryId);

        TrackingGeoLocationServiceImpl geoLocationService = new TrackingGeoLocationServiceImpl(restClientFactory, serviceConfigBean);
        LocationProvider locationProvider = new EbaykLocationProvider(geoLocationService);
        return new Ip2CountryFilterFactory(locationProvider);
    }

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.userstate.enabled", havingValue = "true", matchIfMissing = true)
    public UserStateFilterFactory userStateFilterFactory(
            RestClientFactory restClientFactory,
            @Value("${comaas.filter.ebayservices.userstate.endpointUrl}") String endpointUrl,
            @Value("${comaas.filter.ebayservices.userstate.oauthUrl}") String oauthUrl,
            @Value("${comaas.filter.ebayservices.oauthToken.clientId}") String clientId,
            @Value("${comaas.filter.ebayservices.oauthToken.clientSecret}") String clientSecret
    ) {
        UserStateServiceConfig config = new UserStateServiceConfig(endpointUrl, oauthUrl, clientId, clientSecret);
        UserProfileService userProfileService = new UserProfileServiceImpl(restClientFactory, config);
        return new UserStateFilterFactory(new UserStateProfileBasedProvider(userProfileService));
    }
}
