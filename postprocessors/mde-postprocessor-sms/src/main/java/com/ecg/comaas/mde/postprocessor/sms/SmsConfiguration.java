package com.ecg.comaas.mde.postprocessor.sms;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
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

    @Bean
    public SmsPostProcessor smsPostProcessor() {
        return new SmsPostProcessor(new ContactMessageSmsService(), pluginOrder);
    }

}
