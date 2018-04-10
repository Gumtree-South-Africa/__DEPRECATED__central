package com.ecg.comaas.mde.postprocessor.rememberlastselleraddress;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class SendToLastSellerAddressConfiguration {

    @Bean
    public SendToLastSellerAddressPostProcessor sendToLastSellerAddressPostProcessor() {
        return new SendToLastSellerAddressPostProcessor();
    }
}
