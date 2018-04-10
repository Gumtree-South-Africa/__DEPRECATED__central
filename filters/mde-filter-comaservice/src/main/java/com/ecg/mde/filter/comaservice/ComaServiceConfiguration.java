package com.ecg.mde.filter.comaservice;

import com.ecg.mde.filter.comaservice.filters.ComaFilterService;
import com.ecg.mde.filter.comaservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
public class ComaServiceConfiguration {

    @Value("${replyts.mobile.comafilterservice.webserviceUrl}")
    private String webserviceUrl;

    @Value("${replyts.mobile.comafilterservice.active}")
    private boolean active;

    @Value("${replyts.mobile.comafilterservice.connectTimeout:5000}")
    private int connectionTimeout;

    @Value("${replyts.mobile.comafilterservice.readTimeout:10000}")
    private int readTimeout;

    @Bean
    public ContactMessageAssembler contactMessageAssembler() {
        return new ContactMessageAssembler();
    }

    @Bean
    public FilterServiceFactory filterServiceFactory(ComaFilterService service, ContactMessageAssembler assembler) {
        return new FilterServiceFactory(service, assembler);
    }

    @Bean
    public ComaFilterService comaFilterService() {
        return new ComaFilterService(webserviceUrl, active, connectionTimeout, readTimeout);
    }
}
