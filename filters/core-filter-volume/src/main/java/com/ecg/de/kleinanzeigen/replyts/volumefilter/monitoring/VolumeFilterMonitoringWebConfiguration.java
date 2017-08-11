package com.ecg.de.kleinanzeigen.replyts.volumefilter.monitoring;

import com.ecg.de.kleinanzeigen.replyts.volumefilter.EventStreamProcessor;
import com.ecg.replyts.core.webapi.util.JsonNodeMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class VolumeFilterMonitoringWebConfiguration extends WebMvcConfigurationSupport {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JsonNodeMessageConverter());
    }

    @Bean
    public VolumeFilterMonitoringController volumeFilterMonitoring(EventStreamProcessor eventStreamProcessor) {
        return new VolumeFilterMonitoringController(eventStreamProcessor);
    }
}