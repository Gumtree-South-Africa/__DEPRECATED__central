package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Handles reading and manipulating configuration of plugins. Configurations are read from database. <b>If you are not
 * familiar with the Plugin Configuration system, please get infomration about this first.</b><br/>
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
public class ConfigCrudController implements HandlerExceptionResolver {
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
        return configRepository.getConfigurationsAsJson();
    }

    @ResponseBody
    @RequestMapping(value = "/{pluginFactory}/{instanceId}", method = RequestMethod.PUT, consumes = "*/*")
    public ObjectNode addConfiguration(@PathVariable String pluginFactory, @PathVariable String instanceId, @RequestBody JsonNode body) throws Exception {
        if (instanceId == null || instanceId.isEmpty()) {
            throw new RuntimeException("InstanceId is required");
        }

        configRepository.backupConfigurations();

        PluginConfiguration config = extract(pluginFactory, instanceId, body, body.get("configuration"));

        if (!configUpdateNotifier.validateConfiguration(config)) {
            throw new RuntimeException("PluginFactory " + pluginFactory + " not found");
        }
        LOG.info("Saving Config update {}", config.getId());
        configRepository.persistConfiguration(config);
        configUpdateNotifier.confirmConfigurationUpdate();

        return JsonObjects.builder()
                .attr("pluginFactory", pluginFactory)
                .attr("instanceId", instanceId)
                .success().build();
    }

    @ResponseBody
    @RequestMapping(value = "/", method = RequestMethod.PUT, consumes = "*/*")
    public ObjectNode replaceConfigurations(@RequestBody ArrayNode body) throws Exception {
        List<PluginConfiguration> newConfigurations = verifyConfigurations(body);
        List<PluginConfiguration> currentConfigurations = configRepository.getConfigurations();
        configRepository.backupConfigurations();

        try {
            currentConfigurations.forEach(c -> configRepository.deleteConfiguration(c.getId()));
            newConfigurations.forEach(pluginConfiguration -> configRepository.persistConfiguration(pluginConfiguration));
        } catch (Exception e) {
            LOG.error("Could not save new configurations, attempting to re-save the old ones", e);
            currentConfigurations.forEach(pluginConfiguration -> configRepository.persistConfiguration(pluginConfiguration));
            throw e;
        }

        configUpdateNotifier.confirmConfigurationUpdate();

        return JsonObjects.builder().attr("count", body.size()).success().build();
    }

    @ResponseBody
    @RequestMapping(value = "/{pluginFactory}/{instanceId}", method = RequestMethod.DELETE, consumes = "*/*")
    public ObjectNode deleteConfiguration(@PathVariable String pluginFactory, @PathVariable String instanceId) throws Exception {
        configRepository.backupConfigurations();
        ConfigurationId cid = toId(pluginFactory, instanceId);
        configRepository.deleteConfiguration(cid);
        configUpdateNotifier.confirmConfigurationUpdate();
        return JsonObjects.builder().success().build();

    }

    private List<PluginConfiguration> verifyConfigurations(ArrayNode body) throws Exception {
        List<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        for (JsonNode node : body) {
            assertArrayElement(node, "pluginFactory", TextNode.class);
            assertArrayElement(node, "instanceId", TextNode.class);
            assertArrayElement(node, "configuration", ObjectNode.class);

            String pluginFactory = node.get("pluginFactory").textValue();
            String instanceId = node.get("instanceId").textValue();
            JsonNode configuration = node.get("configuration");

            PluginConfiguration config = extract(pluginFactory, instanceId, configuration, configuration);
            if (!configUpdateNotifier.validateConfiguration(config)) {
                throw new IllegalArgumentException(format("PluginFactory %s not found", pluginFactory));
            }
            pluginConfigurations.add(config);
        }
        return pluginConfigurations;
    }

    private void assertArrayElement(JsonNode element, String fieldName, Class<? extends JsonNode> clazz) {
        if (!(element instanceof ObjectNode)) {
            throw new IllegalArgumentException("Array element is not an Object");
        }

        JsonNode field = element.get(fieldName);

        if (!field.getClass().equals(clazz)) {
            throw new IllegalArgumentException(format("Array element's contents does not contain %s as a %s class", fieldName, clazz));
        }

        if (field instanceof TextNode && !StringUtils.hasText(field.textValue())) {
            throw new IllegalArgumentException(format("Array element's contents contains field %s but it contains an empty value", fieldName));
        }
    }

    private PluginConfiguration extract(String pluginFactory, String instanceId, JsonNode body, JsonNode configuration) throws IllegalArgumentException, ClassNotFoundException {
        if (configuration == null) {
            throw new IllegalArgumentException("payload needs a configuration node where the filter configuration is in");
        }

        Long priority = Long.valueOf(getContent(body, "priority", "0"));
        PluginState state = PluginState.valueOf(getContent(body, "state", PluginState.ENABLED.name()));

        return new PluginConfiguration(toId(pluginFactory, instanceId), priority, state, 1L, configuration);
    }

    private static String getContent(JsonNode body, String fieldname, String alternative) {
        JsonNode fn = body.get(fieldname);
        return fn == null ? alternative : fn.asText();
    }

    private ConfigurationId toId(String pluginFactory, String instanceId) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Class<? extends BasePluginFactory<?>> pluginFactoryClass = (Class<? extends BasePluginFactory<?>>) Class.forName(pluginFactory);
        return new ConfigurationId(pluginFactoryClass, instanceId);
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LOG.error("Config Webservice encountered Exception", ex);
        JsonNode n = JsonObjects.builder().failure(ex).build();
        try {
            String resp = JsonObjects.getObjectMapper().writeValueAsString(n);
            response.setStatus(500);
            response.addHeader("Content-Type", "application/json;charset=utf-8");
            response.getOutputStream().write(resp.getBytes());
            response.getOutputStream().close();
        } catch (Exception e) {
            LOG.error("Error handling error ;) ", e);
        }
        return null;
    }
}
