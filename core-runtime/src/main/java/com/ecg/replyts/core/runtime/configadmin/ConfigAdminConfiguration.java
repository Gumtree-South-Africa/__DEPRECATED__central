package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class ConfigAdminConfiguration {
    @Autowired(required = false)
    private List<FilterFactory> filterFactories = Collections.emptyList();

    @Autowired(required = false)
    private List<ResultInspectorFactory> resultInspectorFactories = Collections.emptyList();

    @Bean(name = "filterConfigurationAdmin")
    public ConfigurationAdmin filterConfigurationAdmin() {
        return new ConfigurationAdmin(filterFactories, "filter-configadmin");
    }

    @Bean
    public Refresher filterRefresher(@Qualifier("filterConfigurationAdmin") ConfigurationAdmin admin) {
        return new Refresher(admin);
    }

    @Bean(name = "resultInspectorConfigurationAdmin")
    public ConfigurationAdmin resultInspectorConfigurationAdmin() {
        return new ConfigurationAdmin(resultInspectorFactories, "resultinspector-configadmin");
    }

    @Bean
    public Refresher resultInspectorRefresher(@Qualifier("resultInspectorConfigurationAdmin") ConfigurationAdmin admin) {
        return new Refresher(admin);
    }
}