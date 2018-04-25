package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.comaas.core.filter.ebayservices.config.UserStateServiceConfig;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import de.mobile.ebay.service.oauthtoken.OAuthProviderConfigBean;
import de.mobile.ebay.service.oauthtoken.OAuthTokenProviderImpl;
import de.mobile.ebay.service.restclient.RestClientFactory;
import de.mobile.ebay.service.restclient.RestClientFactoryBuilder;
import de.mobile.ebay.service.userprofile.UserProfileServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Configuration
@ComponentScan(basePackages = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters")
public class EbayServicesConfiguration {

    @Value("#{systemEnvironment['http_proxy']?:'${replyts2-ebayservicesfilters-plugin.proxyurl:}'}")
    private String ebayServicesProxy;

    @Bean("lbs-esconfig-ipc")
    public de.mobile.ebay.service.lbs.ServiceConfigBean lbsEsconfigIpc(
            @Value("${replyts2-ebayservicesfilters-plugin.trackingGeoLocationService.endpointUrl:https://api.ebay.com/base/location/v2/}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.trackingGeoLocationService.security.clientId}") String clientId,
            @Value("${replyts2-ebayservicesfilters-plugin.trackingGeoLocationService.security.clientSecret}") String clientSecret,
            @Value("${replyts2-ebayservicesfilters-plugin.trackingGeoLocationService.oauthUrl:https://idauth.ebay.com/idauth/site/}") String oAuthUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.trackingGeoLocationService.queryId}") String queryId) {
        de.mobile.ebay.service.lbs.ServiceConfigBean serviceConfigBean = new de.mobile.ebay.service.lbs.ServiceConfigBean();
        serviceConfigBean.setEndpointUrl(endpointUrl);
        serviceConfigBean.setClientId(clientId);
        serviceConfigBean.setClientSecret(clientSecret);
        serviceConfigBean.setOAuthUrl(oAuthUrl);
        serviceConfigBean.setQueryId(queryId);
        return serviceConfigBean;
    }

    @Bean("esconfig-ipc")
    public de.mobile.ebay.service.impl.ServiceConfigBean esconfigIps(
            @Value("${replyts2-ebayservicesfilters-plugin.ip.endpointUrl:https://svcs.ebay.com/ws/spf}") String endpointUrl) {
        de.mobile.ebay.service.impl.ServiceConfigBean serviceConfigBean = new de.mobile.ebay.service.impl.ServiceConfigBean();
        serviceConfigBean.setEndpointUrl(endpointUrl);
        serviceConfigBean.setAppName("");
        serviceConfigBean.setProxyUrl(ebayServicesProxy);
        return serviceConfigBean;
    }

    @Bean("esconfig-ipaddr")
    public de.mobile.ebay.service.impl.ServiceConfigBean esconfigIpaddr(
            @Value("${replyts2-ebayservicesfilters-plugin.ip.endpointUrl:https://svcs.ebay.com/ws/spf}") String endpointUrl) {
        de.mobile.ebay.service.impl.ServiceConfigBean serviceConfigBean = new de.mobile.ebay.service.impl.ServiceConfigBean();
        serviceConfigBean.setEndpointUrl(endpointUrl);
        serviceConfigBean.setAppName("");
        serviceConfigBean.setProxyUrl(ebayServicesProxy);
        return serviceConfigBean;
    }

    @Bean
    public UserProfileServiceImpl ebayUserProfileService(
            @Value("${replyts2-ebayservicesfilters-plugin.user.endpointUrl}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.user.oauthUrl}") String oauthUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientId}") String clientId,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientSecret}") String clientSecret) {
        UserStateServiceConfig config = new UserStateServiceConfig(endpointUrl, oauthUrl, clientId, clientSecret);
        return new UserProfileServiceImpl(ebayServicesRestClientFactory(), config);
    }

    @Bean
    public OAuthTokenProviderImpl ebayOAuthTokenProvider(
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.endpointUrl:https://api.ebay.com/}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientId}") String clientId,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientSecret}") String clientSecret) {
        OAuthProviderConfigBean config = new OAuthProviderConfigBean();
        config.setEndpointUrl(endpointUrl);
        config.setClientId(clientId);
        config.setClientSecret(clientSecret);
        return new OAuthTokenProviderImpl(ebayServicesRestClientFactory(), config);
    }

    private RestClientFactory ebayServicesRestClientFactory() {
        RestClientFactoryBuilder restClientFactoryBuilder = new RestClientFactoryBuilder();
        restClientFactoryBuilder.setMaxTotalConnections(100);
        restClientFactoryBuilder.setSocketTimeout(3);
        restClientFactoryBuilder.setConnectionTimeout(3);
        restClientFactoryBuilder.setProxyUrl(ebayServicesProxy);
        return restClientFactoryBuilder.build();
    }
}
