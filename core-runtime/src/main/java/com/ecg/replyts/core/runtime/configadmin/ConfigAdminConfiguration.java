package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.lang.String.format;

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

    @Bean
    public ConfigurationValidator configurationValidator(List<ConfigurationAdmin<?>> configAdmins) {
        return new ConfigurationValidator(configAdmins);
    }

    @Bean
    public ConfigurationPublisher configurationPublisher(
            @Value("${replyts.tenant.short}") String tenant,
            @Value("${kafka.configurations.topic:configurations}") String kafkaTopic,
            @Value("${kafka.core.servers}") String kafkaEndpoint) {

        return new ConfigurationPublisher(createProducerProperties(kafkaEndpoint), kafkaTopic, tenant);
    }

    @Bean
    public ConfigurationService configurationService(
            ConfigurationRepository repository,
            ConfigurationValidator validator) {

        return new ConfigurationService(repository, validator);
    }

    @Bean
    public ConfigurationConsumer configurationConsumer(
            ConfigurationService service,
            @Value("${kafka.configurations.topic:configurations}") String kafkaTopic,
            @Value("${kafka.core.servers}") String kafkaEndpoint,
            @Value("${replyts.tenant.short}") String tenant,
            @Value("${kafka.configurations.consumes:#{null}}") String consumes) {

        String configurationOwner = consumes == null ? tenant : consumes;
        return new ConfigurationConsumer(service, kafkaTopic, kafkaEndpoint, configurationOwner);
    }

    @Bean
    public ConfigurationConsumerStarter configurationConsumerStarter(
            ConfigurationConsumer configurationConsumer) {

        return new ConfigurationConsumerStarter(configurationConsumer);
    }

    private static Properties createProducerProperties(String bootstrapServers) {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        String allocId = System.getenv("NOMAD_ALLOC_ID");
        if (allocId != null) {
            long threadId = Thread.currentThread().getId();
            props.put(ProducerConfig.CLIENT_ID_CONFIG, format("%s-%d", allocId, threadId));
        }
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return props;
    }
}