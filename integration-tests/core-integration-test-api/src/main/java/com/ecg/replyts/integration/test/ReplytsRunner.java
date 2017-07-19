package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.google.common.io.Files;
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

import java.io.File;
import java.util.*;

import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.setEnv;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ReplytsRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ReplytsRunner.class);

    public static final String DEFAULT_CONFIG_RESOURCE_DIRECTORY = "/integrationtest-conf";

    private static final String ELASTIC_SEARCH_PREFIX = "comaas_integration_test_";

    private final File dropFolder = Files.createTempDir();
    private final Integer httpPort = OpenPortFinder.findFreePort();
    private final Integer smtpOutPort = OpenPortFinder.findFreePort();

    private Wiser wiser = new Wiser(smtpOutPort);
    private AnnotationConfigApplicationContext context;
    private Client searchClient;
    private MailInterceptor mailInterceptor;

    ReplytsRunner(Properties testProperties, String configResourcePrefix, Class<?>[] configurations) {
        wiser.start();

        if (!wiser.getServer().isRunning())
            throw new IllegalStateException("SMTP server thread is not running");

        try {
            context = new AnnotationConfigApplicationContext();

            context.getEnvironment().setActiveProfiles(ReplyTS.EMBEDDED_PROFILE);
            context.register(MailInterceptor.class);

            Properties properties = new Properties();

            properties.put("confDir", "classpath:" + configResourcePrefix);
            properties.put("logDir", "./target");

            ClassPathResource resource = new ClassPathResource(configResourcePrefix + "/replyts.properties");

            properties.load(resource.getInputStream());

            properties.put("persistence.cassandra.core.endpoint", CassandraIntegrationTestProvisioner.getEndPoint());
            properties.put("persistence.cassandra.mb.endpoint", CassandraIntegrationTestProvisioner.getEndPoint());
            setEnv("COMAAS_HTTP_PORT", httpPort.toString());
            properties.put("replyts.ssl.enabled", "false");
            properties.put("delivery.smtp.port", String.valueOf(smtpOutPort));

            properties.put("mailreceiver.filesystem.dropfolder", dropFolder.getAbsolutePath());

            String elasticClusterName = ELASTIC_SEARCH_PREFIX + UUID.randomUUID();
            properties.put("search.es.clustername", elasticClusterName);

            properties.put("node.passive", "true");
            properties.put("cluster.jmx.enabled", "false");

            context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test", properties));

            if (testProperties != null) {
                properties.putAll(testProperties);
            }

            context.registerShutdownHook();

            PathMatchingResourcePatternResolver pmrl = new PathMatchingResourcePatternResolver(context.getClassLoader());
            List<Resource> resources = new ArrayList<>();
            resources.addAll(Arrays.asList(pmrl.getResources("classpath:server-context.xml")));
            resources.addAll(Arrays.asList(pmrl.getResources("classpath:runtime-context.xml")));
            resources.addAll(Arrays.asList(pmrl.getResources("classpath*:/plugin-inf/*.xml")));

            for (Resource r : resources) {
                XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
                reader.loadBeanDefinitions(r);
            }

            if (configurations != null && configurations.length != 0) {
                context.register(configurations);
            }
            context.refresh();

            this.mailInterceptor = checkNotNull(context.getBean(MailInterceptor.class), "mailInterceptor");

            if (!context.getBean("started", Boolean.class)) {
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

    Client getSearchClient() {
        if (searchClient == null) {
            searchClient = context.getBean(Client.class);

            if (searchClient == null) {
                throw new IllegalStateException("COMaaS did not start up in its entirety");
            }
        }

        return searchClient;
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
