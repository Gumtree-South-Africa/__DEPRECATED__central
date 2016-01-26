package com.ecg.replyts.core.api.pluginconfiguration;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base Interface for all Service Producers. Plugin providers will need to implement one of the sub interfaces
 * {@link com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory} or {@link com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory} <strong>and</strong> run as spring beans on themselves.
 * Therefore, this class is not intended for direct use.
 * <p/>
 * If this is the case, they will be asked to instantiate new plugins (filters, postprocessors,...) when asked for.<br/>
 * They need not to take care about the plugin's lifecycling, this is done externally.
 *
 * @param <T> type of plugin to be built.
 * @author mhuttar
 */
public interface BasePluginFactory<T> {
    /**
     * tells the implementing PluginFactory to create a new plugin from a given configuration. Implementations will need
     * to consider this <strong>Not a Spring Bean - dependency injection does not work</strong>. The implementation does
     * not need to take care about updating existing instances versus creating new instances. This is done from the
     * surroundings.
     *
     * @param instanceName  name of the requested instance (instance does not necessarily need to know about htis)
     * @param configuration json based configuration for this service.
     * @return new created service instance with the given configuration.
     */
    T createPlugin(String instanceName, JsonNode configuration);
}
