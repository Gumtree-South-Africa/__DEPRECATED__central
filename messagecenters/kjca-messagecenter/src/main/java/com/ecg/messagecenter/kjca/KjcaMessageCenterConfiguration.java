package com.ecg.messagecenter.kjca;

import com.codahale.metrics.MetricRegistry;
import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.kjca.pushmessage.send.SendPushService;
import com.ecg.messagecenter.kjca.pushmessage.send.client.SendClient;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;

@ComaasPlugin
@Profile(TENANT_KJCA)
@Component
public class KjcaMessageCenterConfiguration {

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public SendClient sendClient(
            @Value("${send.client.max.retries:0}") final Integer maxRetries,
            @Value("${send.client.max.connections:16}") final Integer maxConnections,
            @Value("${send.client.timeout.hystrix.ms:1000}") final Integer hystrixTimeout,
            @Value("${send.client.timeout.socket.millis:1000}") final Integer socketTimeout,
            @Value("${send.client.timeout.connect.millis:1000}") final Integer connectTimeout,
            @Value("${send.client.timeout.connectionRequest.millis:1000}") final Integer connectionRequestTimeout,
            @Value("${send.client.http.schema:http}") final String httpSchema,
            @Value("${send.client.http.endpoint:send-api.clworker.qa10.kjdev.ca}") final String httpEndpoint,
            @Value("${send.client.http.port:80}") final Integer httpPort) {

        return new SendClient(maxRetries, maxConnections, hystrixTimeout, socketTimeout, connectTimeout, connectionRequestTimeout, httpSchema, httpEndpoint, httpPort);
    }

    @Bean
    public SendPushService sendPushService(SendClient sendClient) {
        return new SendPushService(sendClient);
    }

    @Bean
    public TextAnonymizer textAnonymizer(MailCloakingService mailCloakingService) {
        return new TextAnonymizer(mailCloakingService);
    }
}
