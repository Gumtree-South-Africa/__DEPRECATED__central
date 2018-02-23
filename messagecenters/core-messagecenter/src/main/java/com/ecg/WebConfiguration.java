package com.ecg;

import com.ecg.messagebox.util.SwaggerCustomModelPropertyBuilderPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@Import(WebConfiguration.SwaggerConfiguration.class)
@ComponentScan("com.ecg.messagebox.resources")
public class WebConfiguration extends WebMvcConfigurationSupport {

    @Value("${messagebox.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Bean
    @Override
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        RequestMappingHandlerMapping mapping = super.requestMappingHandlerMapping();
        mapping.setAlwaysUseFullPath(true);
        return mapping;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (swaggerEnabled) {
            registry.addResourceHandler("swagger-ui.html")
                    .addResourceLocations("classpath:/META-INF/resources/");

            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }

    @Configuration
    @EnableSwagger2
    @ConditionalOnProperty(name = "messagebox.swagger.enabled", havingValue = "true")
    public class SwaggerConfiguration {

        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                    .select()
                    .apis(RequestHandlerSelectors.basePackage("com.ecg.messagebox.resources"))
                    .build()
                    .useDefaultResponseMessages(false)
                    .apiInfo(metaData());
        }

        /**
         * Disable validation button on Swagger main page.
         */
        @Bean
        public UiConfiguration uiConfig() {
            return UiConfigurationBuilder.builder()
                    .displayRequestDuration(true)
                    .validatorUrl(null)
                    .build();
        }

        private ApiInfo metaData() {
            return new ApiInfoBuilder()
                    .description("Comaas MessageBox (WebAPI v2)")
                    .title("MessageBox")
                    .version("v2")
                    .build();
        }

        /**
         * Hide 'allowEmptyValue' when field annotated with @ApiModelProperty (no officially supported)
         */
        @Bean
        public SwaggerCustomModelPropertyBuilderPlugin swaggerCustomModelPropertyBuilderPlugin() {
            return new SwaggerCustomModelPropertyBuilderPlugin();
        }
    }
}