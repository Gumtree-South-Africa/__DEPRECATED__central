package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.lang.System.getProperty;

public class GoogleAnalyticsServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAnalyticsServiceFactory.class);

    private String trackingId;
    private String gaHostURL;
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    public GoogleAnalyticsService googleAnalyticsService() {
        if (trackingId == null || gaHostURL == null) {
            throw new IllegalArgumentException("Tracking Id and Host URL are mandatory fields");
        }
        return new GoogleAnalyticsService(asyncHttpClient(), trackingId, gaHostURL);
    }

    private AsyncHttpClient asyncHttpClient() {
        return isProxySet() ? new AsyncHttpClient(asyncHttpClientConfig()) : new AsyncHttpClient();
    }

    private AsyncHttpClientConfig asyncHttpClientConfig() {
        logger.debug(String.format("Proxy set using host: %s, port: %s", HTTPS_PROXY_HOST, HTTPS_PROXY_PORT));
        return new AsyncHttpClientConfig.Builder()
                .setProxyServer(
                        new ProxyServer(ProxyServer.Protocol.HTTPS,
                                        getProperty(HTTPS_PROXY_HOST),
                                        Integer.valueOf(getProperty(HTTPS_PROXY_PORT)))
                                .addNonProxyHost("http://localhost")
                )
                .setConnectTimeout(5 * 1000)
                .setRequestTimeout(5 * 1000)
                .setMaxConnectionsPerHost(20)
                .setMaxConnections(100)
//                .setIdleConnectionInPoolTimeoutInMs()
                .build();
    }

    private boolean isProxySet() {
        return Optional.ofNullable(getProperty(HTTPS_PROXY_HOST)).isPresent()
                && Optional.ofNullable(getProperty(HTTPS_PROXY_PORT)).isPresent();
    }

    public void setGaHostURL(String gaHostURL) {
        this.gaHostURL = gaHostURL;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }
}
