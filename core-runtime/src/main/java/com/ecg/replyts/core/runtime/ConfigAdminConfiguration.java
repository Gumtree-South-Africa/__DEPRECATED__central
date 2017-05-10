package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = true)
    private ClusterRefreshPublisher clusterRefreshPublisher;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean(name = "filterConfigurationAdmin")
    public ConfigurationAdmin buildFilterConfigAdmin() {
        return new ConfigurationAdmin(filterFactories, "filter-configadmin", clusterRefreshPublisher);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean(name = "resultInspectorConfigAdmin")
    public ConfigurationAdmin buildResultInspectorConfigAdmin() {
        return new ConfigurationAdmin(resultInspectorFactories, "resultinspector-configadmin", clusterRefreshPublisher);
    }
}
