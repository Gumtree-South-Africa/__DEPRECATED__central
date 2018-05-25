package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Configuration
@Profile(TENANT_GTUK)
public class Replyts2StatsNotifierConfig {

    @Value("${gumtree.stats.api.url}")
    private String statsUrl;

    @Value("${gumtree.analytics.ga.trackingid}")
    private String trackingId;

    @Value("${gumtree.analytics.ga.host}")
    private String gaHost;

    @Value("${gumtree.analytics.ga.enabled}")
    private Boolean sendGoogleAnalyticsEventEnabled;

    @Bean
    public GoogleAnalyticsService googleAnalyticsService() {
        GoogleAnalyticsServiceFactory factory = new GoogleAnalyticsServiceFactory();
        factory.setGaHostURL(gaHost);
        factory.setTrackingId(trackingId);

        return factory.googleAnalyticsService();
    }

    @Bean
    public Replyts2StatsNotifier replyts2StatsNotifier() {
        return new Replyts2StatsNotifier(notificationService(), sendGoogleAnalyticsEventEnabled);
    }

    private NotificationService notificationService() {
        return new NotificationService(googleAnalyticsService(), statsApiNotifier());
    }

    private StatsApiNotifier statsApiNotifier() {
        return new StatsApiNotifier(statsUrl, asyncHttpClient());
    }

    private AsyncHttpClientConfig asyncHttpClientConfig() {
        return new AsyncHttpClientConfig.Builder()
                .setConnectTimeout(10000)
                .setRequestTimeout(10000)
                .build();
    }

    public AsyncHttpClient asyncHttpClient() {
        return new AsyncHttpClient(new GrizzlyAsyncHttpProvider(asyncHttpClientConfig()), asyncHttpClientConfig());
    }
}
