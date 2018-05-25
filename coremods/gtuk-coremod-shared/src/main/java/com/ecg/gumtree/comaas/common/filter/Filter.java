package com.ecg.gumtree.comaas.common.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Filter {
    @JsonProperty
    private String pluginFactory;
    @JsonProperty
    private String instanceId;
    @JsonProperty
    private Object configuration;

    public Filter(String pluginFactory, String instanceId, Object configuration) {
        this.pluginFactory = pluginFactory;
        this.instanceId = instanceId;
        this.configuration = configuration;
    }

    public String getPluginFactory() {
        return pluginFactory;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Object getConfiguration() {
        return configuration;
    }
}
