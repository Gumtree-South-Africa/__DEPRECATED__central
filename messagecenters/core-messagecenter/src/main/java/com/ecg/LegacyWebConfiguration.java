package com.ecg;

import com.ecg.replyts.core.webapi.util.JsonNodeMessageConverter;
import com.ecg.replyts.core.webapi.util.MappingJackson2HttpMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

@Deprecated
@Configuration
public class LegacyWebConfiguration extends WebMvcConfigurationSupport {
    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();

        mapping.setAlwaysUseFullPath(true);

        return mapping;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
        converters.add(new JsonNodeMessageConverter());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.webapi")
    @ConditionalOnExpression(PluginConfiguration.ONLY_V1_TENANTS)
    public static class MessageCenterEndpoints { }

    @Configuration
    @ComponentScan("com.ecg.messagebox.controllers")
    @ConditionalOnExpression(PluginConfiguration.V2_AND_UPGRADE_TENANTS)
    public static class MessageBoxEndpoints { }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.migration")
    @ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid')}")
    public static class MigrationConfiguration { }
}