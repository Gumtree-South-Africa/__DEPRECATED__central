package com.ecg.comaas.gtuk.filter.volume.monitoring;

import com.ecg.comaas.gtuk.filter.volume.EventStreamProcessor;
import com.ecg.replyts.core.webapi.RuntimeExceptionHandler;
import com.ecg.replyts.core.webapi.util.JsonNodeMessageConverter;
import com.ecg.replyts.core.webapi.util.MappingJackson2HttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class VolumeFilterMonitoringWebConfiguration extends WebMvcConfigurationSupport {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new JsonNodeMessageConverter());
    }

    @Bean
    public VolumeFilterMonitoringController volumeFilterMonitoring(EventStreamProcessor eventStreamProcessor) {
        return new VolumeFilterMonitoringController(eventStreamProcessor);
    }

    @Bean
    public RuntimeExceptionHandler runtimeExceptionHandler() {
        return new RuntimeExceptionHandler();
    }
}