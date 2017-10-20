package com.ecg;

import com.ecg.messagecenter.webapi.HttpRequestAccessInterceptor;
import com.ecg.replyts.core.webapi.RuntimeExceptionHandler;
import com.ecg.replyts.core.webapi.util.JsonNodeMessageConverter;
import com.ecg.replyts.core.webapi.util.MappingJackson2HttpMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {
    @Override
    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();

        mapping.setAlwaysUseFullPath(true);

        return mapping;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpRequestAccessInterceptor());
    }

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        exceptionResolvers.add(new ExceptionHandlerExceptionResolver());
    }

    @Bean
    public RuntimeExceptionHandler runtimeExceptionHandler() {
        return new RuntimeExceptionHandler();
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

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        configurer.setNullValue("null");

        return configurer;
    }

    @Configuration
    @ComponentScan("com.ecg.messagebox.controllers")
    @ConditionalOnExpression("#{'${replyts.tenant}' == 'mp' || '${replyts.tenant}' == 'mde'}")
    static class MessageBoxEndpoints { }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.webapi")
    @ConditionalOnExpression("#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde'}")
    static class MessageCenterEndpoints { }

    @Configuration
    @ComponentScan("com.ecg.messagecenter.migration")
    @ConditionalOnExpression("#{'${replyts.tenant}' != 'mp' && '${replyts.tenant}' != 'mde' && '${persistence.strategy}'.startsWith('hybrid')}")
    static class MigrationConfiguration { }
}