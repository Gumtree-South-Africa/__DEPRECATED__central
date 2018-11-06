package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationUtils;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.protobuf.ByteString;
import ecg.unicom.events.configuration.Configuration;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

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
@RestController
public class ConfigCrudController {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigCrudController.class);

    private final boolean kafkaConfigEnabled;
    private final ConfigurationPublisher configKafkaPublisher;
    private final ClusterRefreshPublisher configHazelcastPublisher;
    private final ConfigurationValidator configValidator;
    private final ConfigurationRepository configRepository;

    @Autowired
    public ConfigCrudController(
            @Value("${kafka.configurations.enabled:false}") boolean kafkaConfigEnabled,
            ConfigurationPublisher configKafkaPublisher,
            ClusterRefreshPublisher configHazelcastPublisher,
            ConfigurationValidator configValidator,
            ConfigurationRepository configRepository) {

        this.kafkaConfigEnabled = kafkaConfigEnabled;
        this.configKafkaPublisher = configKafkaPublisher;
        this.configHazelcastPublisher = configHazelcastPublisher;
        this.configValidator = configValidator;
        this.configRepository = configRepository;
    }

    @GetMapping
    public ObjectNode listConfigurations() {
        return configRepository.getConfigurationsAsJson();
    }

    @PutMapping("/{pluginFactory}/{instanceId}")
    public ObjectNode addConfiguration(
            HttpServletRequest request,
            @PathVariable String pluginFactory,
            @PathVariable String instanceId,
            @RequestBody JsonNode body) throws Exception {

        validateConfigurationId(instanceId, pluginFactory);

        PluginConfiguration config = ConfigurationUtils.extract(
                pluginFactory, instanceId, body, body.get("configuration"));

        // Test if the configuration is guaranteed to work
        if (!configValidator.validateConfiguration(config)) {
            throw new RuntimeException("Plugin validation has failed: " + pluginFactory);
        }

        if (kafkaConfigEnabled) {
            LOG.info("Publish an event for saving Configuration. Instance-ID: '{}', Config-ID: '{}', Plugin-Factory: '{}'",
                    instanceId, config.getId(), pluginFactory);

            Configuration.ConfigurationId configurationId =
                    Configuration.ConfigurationId.newBuilder()
                            .setInstanceId(instanceId)
                            .setType(pluginFactory)
                            .build();

            Configuration.ConfigurationCreated configurationCreated =
                    Configuration.ConfigurationCreated.newBuilder()
                            .setConfigurationId(configurationId)
                            .setContent(ByteString.copyFrom(body.toString(), Charsets.UTF_8))
                            .build();

            Configuration.Envelope configurationEnvelope =
                    Configuration.Envelope.newBuilder()
                            .setConfigurationCreated(configurationCreated)
                            .setRemoteAddress(request.getRemoteAddr())
                            .build();

            configKafkaPublisher.publish(configurationEnvelope);
        } else {
            LOG.info("Saving Config update {}", config.getId());
            configRepository.persistConfiguration(config, request.getRemoteAddr());
            configHazelcastPublisher.publish();
        }

        return JsonObjects.builder()
                .attr("pluginFactory", pluginFactory)
                .attr("instanceId", instanceId)
                .success().build();
    }

    @PutMapping
    public ObjectNode replaceConfigurations(
            HttpServletRequest request,
            @RequestBody ArrayNode body) {

        List<PluginConfiguration> newConfigurations = ConfigurationUtils.verify(configValidator, body);

        String configs = newConfigurations.stream()
                .map(PluginConfiguration::getId)
                .map(pluginId -> pluginId.getPluginFactory() + ":" + pluginId.getInstanceId())
                .collect(Collectors.joining(",", "[", "]"));

        LOG.info("Publish an event for replacing Configuration. Count: '{}', Config-IDs: '{}'", body.size(), configs);

        if (kafkaConfigEnabled) {
            Configuration.ConfigurationUpdated configurationUpdated =
                    Configuration.ConfigurationUpdated.newBuilder()
                            .setContent(ByteString.copyFrom(body.toString(), Charsets.UTF_8))
                            .build();

            Configuration.Envelope configurationEnvelope =
                    Configuration.Envelope.newBuilder()
                            .setConfigurationUpdated(configurationUpdated)
                            .setRemoteAddress(request.getRemoteAddr())
                            .build();

            configKafkaPublisher.publish(configurationEnvelope);
        } else {
            configRepository.replaceConfigurations(newConfigurations, request.getRemoteAddr());
            configHazelcastPublisher.publish();
        }

        return JsonObjects.builder()
                .attr("count", body.size())
                .success().build();
    }

    @DeleteMapping("/{pluginFactory}/{instanceId}")
    public ObjectNode deleteConfiguration(
            HttpServletRequest request,
            @PathVariable String pluginFactory,
            @PathVariable String instanceId) {

        LOG.info("Publish an event for deleting a Configuration, Instance-ID '{}', Plugin-Factory: '{}'",
                instanceId, pluginFactory);

        validateConfigurationId(instanceId, pluginFactory);

        if (kafkaConfigEnabled) {
            Configuration.ConfigurationId configurationId =
                    Configuration.ConfigurationId.newBuilder()
                            .setInstanceId(instanceId)
                            .setType(pluginFactory)
                            .build();

            Configuration.ConfigurationDeleted configurationDeleted =
                    Configuration.ConfigurationDeleted.newBuilder()
                            .setConfigurationId(configurationId)
                            .build();

            Configuration.Envelope configurationEnvelope =
                    Configuration.Envelope.newBuilder()
                            .setRemoteAddress(request.getRemoteAddr())
                            .setConfigurationDeleted(configurationDeleted)
                            .build();

            configKafkaPublisher.publish(configurationEnvelope);
        } else {
            configRepository.deleteConfiguration(pluginFactory, instanceId, request.getRemoteAddr());
            configHazelcastPublisher.publish();
        }

        return JsonObjects.builder()
                .success().build();
    }

    private static void validateConfigurationId(String instanceId, String pluginFactory) {
        if (Strings.isBlank(instanceId) || Strings.isBlank(pluginFactory)) {
            String message = String.format("InstanceId and PluginFactory are mandatory fields: InstanceId: '%s', PluginFactory: '%s'",
                    instanceId, pluginFactory);
            throw new RuntimeException(message);
        }
    }
}
