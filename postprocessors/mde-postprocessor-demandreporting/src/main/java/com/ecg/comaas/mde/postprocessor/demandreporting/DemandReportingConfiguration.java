package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.usertracking.TrackingEventPublisherFactory;
import com.ecg.comaas.mde.postprocessor.demandreporting.usertracking.UserTrackingHandler;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.google.gson.Gson;
import de.mobile.reporting.demand.client.internal.MongoDBWritingReportingClient;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.UnknownHostException;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class DemandReportingConfiguration {

    @Value("${reporting.mongodb.hosts}")
    private String mongoDbHosts;

    @Value("${reporting.mongodb.concurrency}")
    private int mongoDbConcurrency;

    @Value("${mobilede.demand.reporting.plugin.order}")
    private int pluginOrder;

    @Bean(destroyMethod = "shutdown")
    public MongoDBWritingReportingClient mongoDBWritingReportingClient() throws UnknownHostException {
        return new MongoDBWritingReportingClient(mongoDbHosts, mongoDbConcurrency);
    }

    @Bean
    public Gson gsonDemandConfig() {
        return GsonDemandConfig.createConfiguredGson();
    }

    @Bean(destroyMethod = "shutdownClient")
    public HttpClientProvider httpClientProvider() {
        return new HttpClientProvider();
    }

    @Bean
    public HttpClient httpClient(HttpClientProvider httpClientProvider) {
        return httpClientProvider.provideClient();
    }

    @Bean
    public BehaviorTrackingHandler.Config behaviorTrackingHandlerConfig() {
        return new BehaviorTrackingHandler.Config();
    }

    @Bean(destroyMethod = "flushOnExit")
    public BehaviorTrackingHandler behaviorTrackingHandler(BehaviorTrackingHandler.Config config, Gson gson, HttpClient httpClient) {
        return new BehaviorTrackingHandler(config, new HttpEventPublisherFactory(config, httpClient, gson));
    }

    @Bean
    public UserTrackingHandler.Config userTrackingHandlerConfig() {
        return new UserTrackingHandler.Config();
    }

    @Bean(destroyMethod = "flushOnExit")
    public UserTrackingHandler userTrackingHandler(UserTrackingHandler.Config config, Gson gson, HttpClient httpClient) {
        return new UserTrackingHandler(config, new TrackingEventPublisherFactory(config, httpClient, gson));
    }

    @Bean
    public DemandReportingPostProcessor demandReportingPostProcessor(
            MongoDBWritingReportingClient reportingClient,
            BehaviorTrackingHandler behavoirTrackingHandler,
            UserTrackingHandler userTrackingHandler) {

        return new DemandReportingPostProcessor(
                new DemandReportingHandlerFactory(reportingClient), behavoirTrackingHandler, userTrackingHandler, pluginOrder);
    }
}
