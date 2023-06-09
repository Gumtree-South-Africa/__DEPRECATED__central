package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Handles reading and manipulating configuration of plugins. Configurations are read from database. <b>If you are not
 * familiar with the Plugin Configuration system, please get information about this first.</b><br/>
 * Supported Operations are:
 * <p/>
 * <h1>Read</h1>
 * <p/>
 * <pre>
 * curl -i -H "Accept: Application/Json" -H "Content-Type: Application/Json" -X GET http://localhost:8081/configv2/
 * </pre>
 * <p/>
 * Retrieve a listing of all configurations with data that are persisted right now. This does not guarantee that those
 * configurations are valid right now and up and running. (Even tough Create/Update verifies that no configuration of
 * such can be persisted; configurations that are persisted already might become invalid when the plugin code itself
 * changes)
 * <p/>
 * <h1>Create/Update</h1>
 * <p/>
 * <pre>
 * curl -i -H "Accept: Application/Json" -H "Content-Type: Application/Json" -X PUT -d "{'configuration': {}, priority: 24, state: 'EVALUATION'}" http://localhost:8081/configv2/com.ecg.replyts.core.runtime.SampleFilterfactory/test
 * </pre>
 * <p/>
 * Creates new configurations/updates existing configurations (transparently). The Path where the configuration is put
 * is assembled from the class name of the Plugin Factory plus the instanceId. (Both together form the unique identifier
 * of a plugin configuration). The Payload is a json object containing those fields:
 * <dl>
 * <dt>configuration (required)</dt>
 * <dd>actual configuration data that is passed to the plugin instance as configuration. Must be a json object itself.</dd>
 * <dt>priority (optional)</dt>
 * <dd>Priority of the plugin instance. all plugins of a specific type (e.g. filters) are executed in order of their
 * priority (highest value first). Defaults to a priority of 0. Value must be in java's long range. (64bits signed)</dd>
 * <dt>state (optional)</dt>
 * <dd>state of the plugin. Must be a valid {@link PluginState}. Defaults to {@link PluginState#ENABLED}.</dd>
 * </dl>
 * Before a configuration is persisted, it will be tested. Meaning that ReplyTS will try to ensure that the specified
 * pluginFactory exists and is able to create a plugin with the given configuration content. (If plugin creation fails
 * due to an exception or the plugin factory is not found, the configuration is rejected).
 * <p/>
 * <h1>Delete</h1>
 * <p/>
 * <pre>
 * curl -i -H "Accept: Application/Json" -H "Content-Type: Application/Json" -X DELETE  http://localhost:8081/configv2/com.ecg.replyts.core.runtime.SampleFilterfactory/test
 * </pre>
 * <p/>
 * Deletes the configuration identified by it's factoryClass and instanceId from persistence layer. Will not validate if
 * this configuration exists.
 *
 * @author mhuttar
 */
@Controller
public class ConfigCrudController {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigCrudController.class);

    private ConfigurationRepository configRepository;

    private ConfigurationUpdateNotifier configUpdateNotifier;

    @Autowired
    ConfigCrudController(ConfigurationRepository repository, ConfigurationUpdateNotifier updateNotifier) {
        configRepository = repository;
        configUpdateNotifier = updateNotifier;
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ObjectNode listConfigurations() {
        return ConfigApiJsonMapper.Model.toJsonPluginConfigurationList(configRepository.getConfigurations());
    }

    /**
     * This is used by the externalized Filter, which does not get access to Cassandra directly.
     */
    @ResponseBody
    @RequestMapping(value = "/filters", method = RequestMethod.GET)
    public Map<UUID, ObjectNode> getConfigurationsMap() {
        return configRepository.getConfigurations().stream()
                .collect(Collectors.toMap(
                        PluginConfiguration::getUuid,
                        ConfigApiJsonMapper.Model::toJsonPluginConfig
                ));
    }

    @ResponseBody
    @RequestMapping(value = "/{pluginFactory}/{instanceId}", method = RequestMethod.PUT, consumes = "*/*")
    public ObjectNode addConfiguration(HttpServletRequest request, @PathVariable String pluginFactory,
                                       @PathVariable String instanceId, @RequestBody JsonNode body) throws Exception {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new RuntimeException("InstanceId is required");
        }

        PluginConfiguration config = ConfigApiJsonMapper.Json.toPluginConfiguration(pluginFactory, instanceId, body, body.get("configuration"));

        if (!configUpdateNotifier.validateConfiguration(config)) {
            throw new RuntimeException("Plugin validation has failed: " + pluginFactory);
        }
        LOG.info("Saving Config update {}", config.getId());
        configRepository.persistConfiguration(config, request.getRemoteAddr());
        configUpdateNotifier.confirmConfigurationUpdate();

        return JsonObjects.builder()
                .attr("pluginFactory", pluginFactory)
                .attr("instanceId", instanceId)
                .success().build();
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = "*/*")
    public ObjectNode replaceConfigurations(HttpServletRequest request, @RequestBody ArrayNode body) throws Exception {
        List<PluginConfiguration> newConfigurations = ConfigApiJsonMapper.Json.toPluginConfigurationList(body);

        newConfigurations.stream().forEach(config -> {
            if (!configUpdateNotifier.validateConfiguration(config)) {
                throw new IllegalArgumentException(format("PluginFactory %s not found", config.getId().getPluginFactory()));
            }
        });

        configRepository.replaceConfigurations(newConfigurations, request.getRemoteAddr());

        configUpdateNotifier.confirmConfigurationUpdate();

        return JsonObjects.builder().attr("count", body.size()).success().build();
    }

    @ResponseBody
    @RequestMapping(value = "/{pluginFactory}/{instanceId}", method = RequestMethod.DELETE, consumes = "*/*")
    public ObjectNode deleteConfiguration(HttpServletRequest request, @PathVariable String pluginFactory, @PathVariable String instanceId) throws Exception {
        configRepository.deleteConfiguration(pluginFactory, instanceId, request.getRemoteAddr());
        configUpdateNotifier.confirmConfigurationUpdate();
        return JsonObjects.builder().success().build();

    }


}
