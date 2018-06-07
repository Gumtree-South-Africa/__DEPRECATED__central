package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.ApplicationReadyEvent;
import com.ecg.replyts.core.LoggingService;
import com.ecg.replyts.core.Webserver;
import com.ecg.replyts.core.runtime.indexer.IndexDataBuilder;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.io.Files;
import io.prometheus.client.CollectorRegistry;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.ecg.replyts.core.api.model.Tenants.TENANT;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.setEnv;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ReplytsRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ReplytsRunner.class);

    private static final String ELASTIC_SEARCH_PREFIX = "comaas_integration_test_";

    private final File dropFolder = Files.createTempDir();

    private final Integer httpPort = OpenPortFinder.findFreePort();

    private final Integer smtpOutPort = OpenPortFinder.findFreePort();

    private final Wiser wiser = new Wiser(smtpOutPort);

    private final AnnotationConfigApplicationContext context;

    private Client searchClient;

    private DirectESIndexer esIndexer;

    private final MailInterceptor mailInterceptor;

    ReplytsRunner(Properties testProperties, String configResourcePrefix, Class<?>[] configurations) {
        wiser.start();

        try {
            CollectorRegistry.defaultRegistry.clear();

            context = new AnnotationConfigApplicationContext();

            Properties properties = new Properties();

            properties.put("logDir", "./target");

            ClassPathResource resource = new ClassPathResource(configResourcePrefix + "/replyts.properties");

            properties.load(resource.getInputStream());

            properties.put("persistence.cassandra.core.endpoint", CassandraIntegrationTestProvisioner.getEndPoint());
            properties.put("persistence.cassandra.mb.endpoint", CassandraIntegrationTestProvisioner.getEndPoint());
            setEnv("COMAAS_HTTP_PORT", httpPort.toString());
            properties.put("delivery.smtp.port", String.valueOf(smtpOutPort));
            properties.put("kafka.core.servers", "localhost:9092");

            properties.put("mailreceiver.filesystem.dropfolder", dropFolder.getAbsolutePath());

            String elasticClusterName = ELASTIC_SEARCH_PREFIX + UUID.randomUUID();
            properties.put("search.es.clustername", elasticClusterName);
            Boolean isESEnabled = (Boolean) testProperties.get("search.es.enabled");
            if (isESEnabled != null && isESEnabled) {
                searchClient = getElasticSearchClient("elasticsearch");
            }

            properties.put("node.run.cronjobs", "false");
            properties.put("cluster.jmx.enabled", "false");

            context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test", properties));

            if (testProperties != null) {
                properties.putAll(testProperties);
            }

            context.register(MailInterceptor.class);
            context.register(LoggingService.class);

            Resource megaTemporaryResource = new PathMatchingResourcePatternResolver(context.getClassLoader()).getResources("classpath:super-mega-temporary-context.xml")[0];

            new XmlBeanDefinitionReader(context).loadBeanDefinitions(megaTemporaryResource);

            if (configurations != null && configurations.length != 0) {
                LOG.info("Registering: {}", configurations);
                context.register(configurations);
            }

            context.registerShutdownHook();

            context.getEnvironment().setActiveProfiles(testProperties.getProperty(TENANT));

            context.refresh();

            this.mailInterceptor = checkNotNull(context.getBean(MailInterceptor.class), "mailInterceptor");

            context.publishEvent(new ApplicationReadyEvent(context));

            if (!context.getBean(Webserver.class).isStarted()) {
                throw new IllegalStateException("COMaaS did not start up in its entirety");
            }
        } catch (Exception e) {
            throw new IllegalStateException("COMaaS Abnormal Shutdown", e);
        }
    }

    public void stop() {
        context.close();

        deleteDirectory(dropFolder);

        wiser.stop();
    }

    public List<WiserMessage> getMessages() {
        return wiser.getMessages();
    }

    public int getHttpPort() {
        return httpPort;
    }


    private Client getElasticSearchClient(String clusterName) {
        if(Strings.isNullOrEmpty(clusterName)) {
            throw new IllegalArgumentException("Must have a clusterName");
        }
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        Settings settings = Settings.builder().put("cluster.name", clusterName).build();

        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300)));
        return client;
    }

    Client getSearchClient() {
            if (searchClient == null) {
                throw new IllegalStateException("COMaaS did not start up in its entirety or ElasticSearch was not enabled");
            }
        return searchClient;
    }

    DirectESIndexer getESIndexer() {
        if (esIndexer == null) {
            IndexDataBuilder indexDataBuilder = context.getBean(IndexDataBuilder.class);

            if (indexDataBuilder == null) {
                throw new IllegalStateException("COMaaS did not start up in its entirety");
            }
            esIndexer = new DirectESIndexer(getSearchClient(), indexDataBuilder);
        }
        return esIndexer;
    }

    File getDropFolder() {
        return dropFolder;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        LOG.warn("Could not delete file {}", file);
                    }
                }
            }
        }
        if (!directory.delete()) {
            LOG.warn("Could not delete directory {}", directory);
        }
    }

    MailInterceptor getMailInterceptor() {
        return mailInterceptor;
    }
}
