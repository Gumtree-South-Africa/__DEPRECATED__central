package com.ecg.comaas.gtau.filter.ebayservices;

import com.ebay.ecg.gumtree.australia.lbs2.config.TrackingGeolocationConfiguration;
import com.ecg.comaas.gtau.filter.ebayservices.ip2country.Ip2CountryFilterFactory;
import com.ecg.comaas.gtau.filter.ebayservices.ip2country.Ip2CountryResolver;
import com.ecg.comaas.gtau.filter.ebayservices.iprisk.IpRiskFilterFactory;
import com.ecg.comaas.gtau.filter.ebayservices.userstate.UserStateFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.google.common.base.Strings;
import de.mobile.ebay.service.impl.ServiceConfigBean;
import de.mobile.ebay.service.impl.UserServiceStaticTokenProvider;
import de.mobile.ebay.service.oauthtoken.OAuthProviderConfigBean;
import de.mobile.ebay.service.oauthtoken.OAuthTokenProviderImpl;
import de.mobile.ebay.service.restclient.RestClientFactoryBuilder;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@ComaasPlugin
@Configuration
public class EbayServicesConfiguration {

    @Bean
    public Ip2CountryFilterFactory ip2CountryFilterFactory(
            @Value("${replyts2-ebayservicesfilters-plugin.ip.authUrl:}") String authUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.url:}") String url,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.queryId:}") String queryId,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.proxyHost:}") String proxyHost,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.proxyPort:}") Integer proxyPort) {

        TrackingGeolocationConfiguration config = new TrackingGeolocationConfiguration(authUrl, url, queryId, proxyHost, proxyPort);
        return new Ip2CountryFilterFactory(new Ip2CountryResolver(config));
    }

    @Bean
    public IpRiskFilterFactory ipRiskFilterFactory(
            OAuthTokenProviderImpl tokenProvider,
            @Qualifier("ebayServicesHttpClient") HttpClient httpClient,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.endpointUrl:https://svcs.ebay.com/ws/spf}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.ip.appName:}") String appName,
            @Value("${replyts2-ebayservicesfilters-plugin.proxyurl:}") String proxyUrl) {

        ServiceConfigBean config = new ServiceConfigBean();
        config.setEndpointUrl(endpointUrl);
        config.setAppName(appName);
        config.setProxyUrl(proxyUrl);

        return new IpRiskFilterFactory(httpClient, config, tokenProvider);
    }

    @Bean
    public UserStateFilterFactory userStateFilterFactory(
            @Qualifier("ebayServicesHttpClient") HttpClient httpClient,
            @Value("${replyts2-ebayservicesfilters-plugin.user.iafToken:}") String ebayServiceToken,
            @Value("${replyts2-ebayservicesfilters-plugin.user.endpointUrl:https://svcs.ebay.com/ws/spf}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.user.appName:}") String appName,
            @Value("${replyts2-ebayservicesfilters-plugin.proxyurl:}") String proxyUrl) {

        ServiceConfigBean config = new ServiceConfigBean();
        config.setEndpointUrl(endpointUrl);
        config.setAppName(appName);
        config.setProxyUrl(proxyUrl);

        return new UserStateFilterFactory(httpClient, config, new UserServiceStaticTokenProvider(ebayServiceToken));
    }

    @Bean
    public OAuthTokenProviderImpl oAuthTokenProvider(
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.endpointUrl:https://api.ebay.com/}") String endpointUrl,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientId:}") String clientId,
            @Value("${replyts2-ebayservicesfilters-plugin.oauthtoken.clientSecret:}") String clientSecret,
            @Value("${replyts2-ebayservicesfilters-plugin.proxyurl:}") String ebayServicesProxyUrl) {

        RestClientFactoryBuilder clientBuilder = new RestClientFactoryBuilder();
        clientBuilder.setMaxTotalConnections(100);
        clientBuilder.setConnectionTimeout(3);
        clientBuilder.setSocketTimeout(3);
        clientBuilder.setProxyUrl(ebayServicesProxyUrl);

        OAuthProviderConfigBean oAuthConfig = new OAuthProviderConfigBean();
        oAuthConfig.setEndpointUrl(endpointUrl);
        oAuthConfig.setClientId(clientId);
        oAuthConfig.setClientSecret(clientSecret);

        return new OAuthTokenProviderImpl(clientBuilder.build(), oAuthConfig);
    }

    @Bean
    public HttpClient ebayServicesHttpClient(
            PoolingClientConnectionManager poolingConnectionManager,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.connectionTimeoutMs:4000}") int connectionTimeout,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.socketTimeoutMs:4000}") int socketTimeout,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.proxy.host:}") String proxyHost,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.proxy.port:0}") Integer proxyPort) {

        HttpParams clientParams = new BasicHttpParams();
        clientParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        clientParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
        if (!Strings.isNullOrEmpty(proxyHost)) {
            clientParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(proxyHost, proxyPort));
        }

        return new DefaultHttpClient(poolingConnectionManager, clientParams);
    }

    @Bean(destroyMethod = "shutdown")
    public PoolingClientConnectionManager poolingClientConnectionManager(
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionTtlSeconds:120}") int maxConnectionTtlSeconds,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionsPerRoute:30}") int maxConnectionsPerRoute,
            @Value("${replyts2-ebayservicesfilters-plugin.httpclient.maxConnectionsTotal:100}") int maxConnections) {

        PoolingClientConnectionManager poolingConnectionManager =
                new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault(), maxConnectionTtlSeconds, TimeUnit.SECONDS);
        poolingConnectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        poolingConnectionManager.setMaxTotal(maxConnections);
        return poolingConnectionManager;
    }
}
