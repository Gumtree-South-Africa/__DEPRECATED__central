package com.ecg.comaas.mde.postprocessor.sms;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class SmsConfiguration {

    @Value("${mobilede.sms.plugin.order}")
    private int pluginOrder;

    @Value("${mde.sms.service.url:}")
    private String apiUrl;

    @Bean
    public SmsPostProcessor smsPostProcessor(HttpClient httpClient) {
        return new SmsPostProcessor(new ContactMessageSmsService(httpClient, apiUrl), pluginOrder);
    }

}
