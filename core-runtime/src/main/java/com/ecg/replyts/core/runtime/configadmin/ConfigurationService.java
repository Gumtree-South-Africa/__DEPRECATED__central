package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.protobuf.ByteString;
import ecg.unicom.events.configuration.Configuration;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationService.class);

    private final ConfigurationRepository configurationRepository;
    private final ConfigurationValidator configurationValidator;

    public ConfigurationService(
            ConfigurationRepository configurationRepository,
            ConfigurationValidator configurationValidator) {

        this.configurationRepository = configurationRepository;
        this.configurationValidator = configurationValidator;
    }

    public void processEvent(Configuration.Envelope envelope) {
        try {
            if (envelope.hasConfigurationCreated()) {
                createConfiguration(envelope);
            } else if (envelope.hasConfigurationUpdated()) {
                updateConfiguration(envelope);
            } else if (envelope.hasConfigurationDeleted()) {
                deleteConfiguration(envelope);
            }
        } catch (Exception ex) {
            LOG.error("Error occured during processing configuration events", ex);
        }
    }

    private void createConfiguration(Configuration.Envelope envelope) {
        Configuration.ConfigurationCreated event = envelope.getConfigurationCreated();
        Configuration.ConfigurationId configId = event.getConfigurationId();
        validateConfigurationId(configId);

        String instanceId = configId.getInstanceId();
        String pluginFactory = configId.getType();

        JsonNode contentJson = getJsonContent(event.getContent(), instanceId, pluginFactory);
        PluginConfiguration config = ConfigurationUtils.extract(pluginFactory, instanceId, contentJson, contentJson.get("configuration"));

        // Test if the configuration is guaranteed to work
        if (!configurationValidator.validateConfiguration(config)) {
            throw new RuntimeException("Plugin validation has failed: " + pluginFactory);
        }
        LOG.info("Consuming an event for saving Configuration. Instance-ID: '{}', Config-ID: '{}', Plugin-Factory: '{}'",
                instanceId, config.getId(), pluginFactory);

        configurationRepository.persistConfiguration(config, envelope.getRemoteAddress());
    }

    private void updateConfiguration(Configuration.Envelope envelope) {
        Configuration.ConfigurationUpdated event = envelope.getConfigurationUpdated();
        JsonNode contentJson = getJsonContent(event.getContent(), null, null);

        List<PluginConfiguration> newConfigs;
        if (contentJson instanceof ArrayNode) {
            newConfigs = ConfigurationUtils.verify(configurationValidator, (ArrayNode) contentJson);
        } else {
            throw new RuntimeException("Body parameter is not an instance of " + ArrayNode.class.getSimpleName());
        }

        String configIds = newConfigs.stream()
                .map(PluginConfiguration::getId)
                .map(pluginId -> pluginId.getPluginFactory() + ":" + pluginId.getInstanceId())
                .collect(Collectors.joining(",", "[", "]"));

        LOG.info("Publish an event for replacing Configuration. Count: '{}', Config-IDs: '{}'", newConfigs.size(), configIds);

        configurationRepository.replaceConfigurations(newConfigs, envelope.getRemoteAddress());
    }

    private void deleteConfiguration(Configuration.Envelope envelope) {
        Configuration.ConfigurationDeleted event = envelope.getConfigurationDeleted();
        Configuration.ConfigurationId configId = event.getConfigurationId();
        validateConfigurationId(configId);

        LOG.info("Publish an event for deleting a Configuration, Instance-ID '{}', Plugin-Factory: '{}'",
                configId.getInstanceId(), configId.getType());

        configurationRepository.deleteConfiguration(configId.getType(), configId.getInstanceId(), envelope.getRemoteAddress());
    }

    private static JsonNode getJsonContent(ByteString protoContent, String instanceId, String pluginFactory) {
        try {
            return ObjectMapperConfigurer.getObjectMapper().readTree(protoContent.toStringUtf8());
        } catch (IOException e) {
            String message = String.format("Could not convert string to json: InstanceId: '%s', PluginFactory: '%s'",
                    instanceId, pluginFactory);
            throw new RuntimeException(message, e);
        }
    }

    private static void validateConfigurationId(Configuration.ConfigurationId configId) {
        if (Strings.isBlank(configId.getInstanceId()) || Strings.isBlank(configId.getType())) {
            String message = String.format("InstanceId and PluginFactory are mandatory fields: InstanceId: '%s', PluginFactory: '%s'",
                    configId.getInstanceId(), configId.getType());
            throw new RuntimeException(message);
        }
    }
}
