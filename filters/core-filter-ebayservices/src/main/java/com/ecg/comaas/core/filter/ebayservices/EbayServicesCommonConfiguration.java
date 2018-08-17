package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.comaas.core.filter.ebayservices.iprisk.IpRiskFilterFactory;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import com.google.common.base.Strings;
import de.mobile.ebay.service.OAuthTokenProvider;
import de.mobile.ebay.service.oauthtoken.OAuthProviderConfigBean;
import de.mobile.ebay.service.oauthtoken.OAuthTokenProviderImpl;
import de.mobile.ebay.service.restclient.RestClientFactory;
import de.mobile.ebay.service.restclient.RestClientFactoryBuilder;
import okhttp3.OkHttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EbayServicesCommonConfiguration {

    private static HttpClientMockInterceptor mockInterceptor = new HttpClientMockInterceptor();

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.iprisk.enabled", havingValue = "true", matchIfMissing = true)
    public IpRiskFilterFactory ipRiskFilterFactory(
            CloseableHttpClient httpClient,
            OAuthTokenProvider oAuthTokenProvider,
            @Value("${comaas.filter.ebayservices.iprisk.endpointUrl}") String ipEndpointUrl,
            @Value("${comaas.filter.ebayservices.iprisk.appName:}") String appName,
            @Value("${comaas.filter.ebayservices.proxyUrl:}") String ebayServicesProxyUrl
    ) {
        de.mobile.ebay.service.impl.ServiceConfigBean serviceConfigBean = new de.mobile.ebay.service.impl.ServiceConfigBean();
        serviceConfigBean.setEndpointUrl(ipEndpointUrl);
        serviceConfigBean.setAppName(appName);
        serviceConfigBean.setProxyUrl(ebayServicesProxyUrl);

        return new IpRiskFilterFactory(httpClient, serviceConfigBean, oAuthTokenProvider);
    }

    @Bean
    @ConditionalOnProperty(value = "comaas.filter.ebayservices.iprisk.enabled", havingValue = "true", matchIfMissing = true)
    public OAuthTokenProvider oAuthTokenProvider(
            RestClientFactory restClientFactory,
            @Value("${comaas.filter.ebayservices.oauthToken.endpointUrl}") String endpointUrl,
            @Value("${comaas.filter.ebayservices.oauthToken.clientId:}") String clientId,
            @Value("${comaas.filter.ebayservices.oauthToken.clientSecret:}") String clientSecret
    ) {
        OAuthProviderConfigBean oAuthConfig = new OAuthProviderConfigBean();
        oAuthConfig.setEndpointUrl(endpointUrl);
        oAuthConfig.setClientId(clientId);
        oAuthConfig.setClientSecret(clientSecret);
        return new OAuthTokenProviderImpl(restClientFactory, oAuthConfig);
    }

    @Bean
    public RestClientFactory ebayServicesRestClientFactory(@Value("${comaas.filter.ebayservices.proxyUrl:}") String ebayServicesProxyUrl,
                                                           @Value("${comaas.filter.ebayservices.mock.http.client:false}") Boolean mockHttpClient) {
        if (mockHttpClient) {
            return () -> new OkHttpClient.Builder().addInterceptor(mockInterceptor);
        }

        RestClientFactoryBuilder restClientFactoryBuilder = new RestClientFactoryBuilder();
        restClientFactoryBuilder.setMaxTotalConnections(100);
        restClientFactoryBuilder.setSocketTimeout(3);
        restClientFactoryBuilder.setConnectionTimeout(3);
        restClientFactoryBuilder.setProxyUrl(ebayServicesProxyUrl);
        return restClientFactoryBuilder.build();
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient httpClient(
            @Value("${comaas.filter.ebayservices.http.connectionTimeoutMs:4000}") int connectionTimeout,
            @Value("${comaas.filter.ebayservices.http.socketTimeoutMs:4000}") int socketTimeout,
            @Value("${comaas.filter.ebayservices.http.maxConnectionsPerRoute:30}") int maxConnectionsPerRoute,
            @Value("${comaas.filter.ebayservices.http.maxConnectionsTotal:100}") int maxConnections,
            @Value("${comaas.filter.ebayservices.http.proxy.host:}") String proxyHost,
            @Value("${comaas.filter.ebayservices.http.proxy.port:0}") int proxyPort,
            @Value("${comaas.filter.ebayservices.mock.http.client:false}") Boolean mockHttpClient
    ) {
        CloseableHttpClient closeableHttpClient = Strings.isNullOrEmpty(proxyHost)
                ? HttpClientFactory.createCloseableHttpClient(connectionTimeout, connectionTimeout, socketTimeout, maxConnectionsPerRoute, maxConnections)
                : HttpClientFactory.createCloseableHttpClientWithProxy(connectionTimeout, connectionTimeout, socketTimeout, maxConnectionsPerRoute, maxConnections, proxyHost, proxyPort);

        return mockHttpClient
                ? new HttpMockClient(closeableHttpClient)
                : closeableHttpClient;
    }

    public static HttpClientMockInterceptor getMockInterceptor() {
        return mockInterceptor;
    }
}
