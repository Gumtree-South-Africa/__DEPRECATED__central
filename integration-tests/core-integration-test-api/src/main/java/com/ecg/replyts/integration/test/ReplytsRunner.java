package com.ecg.replyts.integration.test;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.ApplicationReadyEvent;
import com.ecg.replyts.core.LoggingService;
import com.ecg.replyts.core.Webserver;
import com.ecg.replyts.core.runtime.indexer.test.DirectESIndexer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import io.prometheus.client.CollectorRegistry;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import java.util.List;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.setEnv;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ReplytsRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ReplytsRunner.class);

    private final Integer httpPort = OpenPortFinder.findFreePort();

    private final Integer smtpOutPort = OpenPortFinder.findFreePort();

    private final Wiser wiser = new Wiser(smtpOutPort);

    private final MessageProcessingCoordinator messageProcessingCoordinator;

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
            properties.put("doc2kafka.sink.enabled", false);
            properties.put("search.es.indexname", "comaasidx");
            properties.put("search.es.endpoint","http://localhost:9200");

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
            this.messageProcessingCoordinator = context.getBean(MessageProcessingCoordinator.class);

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
        wiser.stop();
    }

    public List<WiserMessage> getMessages() {
        return wiser.getMessages();
    }

    public int getHttpPort() {
        return httpPort;
    }

    Client getSearchClient() {
        if (searchClient == null) {
            searchClient = context.getBean(Client.class);

            if (searchClient == null) {
                throw new IllegalStateException("COMaaS did not start up in its entirety or ElasticSearch was not enabled");
            }
        }
        return searchClient;
    }

    DirectESIndexer getESIndexer() {
        if (esIndexer == null) {
            esIndexer = context.getBean(DirectESIndexer.class);

            if (esIndexer == null) {
                throw new IllegalStateException("COMaaS did not start up in its entirety");
            }
        }
        return esIndexer;
    }

    MailInterceptor getMailInterceptor() {
        return mailInterceptor;
    }

    MessageProcessingCoordinator getMessageProcessingCoordinator() {
        return messageProcessingCoordinator;
    }
}
