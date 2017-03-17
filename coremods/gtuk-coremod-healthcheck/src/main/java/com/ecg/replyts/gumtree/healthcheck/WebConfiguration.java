package com.ecg.replyts.gumtree.healthcheck;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@ComponentScan("com.ecg.replyts.gumtree.healthcheck")
public class WebConfiguration extends WebMvcConfigurationSupport {
}