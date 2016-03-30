package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import com.google.common.io.Files;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.elasticsearch.client.Client;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public final class ReplytsRunner {
    public static final String DEFAULT_CONFIG_RESOURCE_DIRECTORY = "/integrationtest-conf";

    private static final String ELASTIC_SEARCH_PREFIX = "comaas_integration_test_";

    private final File dropFolder = Files.createTempDir();
    private final Integer httpPort = OpenPortFinder.findFreePort();
    private final Integer smtpOutPort = OpenPortFinder.findFreePort();
    private final String elasticClusterName = ELASTIC_SEARCH_PREFIX + UUID.randomUUID();

    private Wiser wiser = new Wiser(smtpOutPort);

    private AbstractApplicationContext context;

    private Client searchClient;

    public ReplytsRunner(Properties testProperties, String configResourcePrefix) {
        wiser.start();

        if (!wiser.getServer().isRunning())
            throw new IllegalStateException("SMTP server thread is not running");

        try {
            context = new ClassPathXmlApplicationContext(new String[] {
                "classpath:server-context.xml",
                "classpath:runtime-context.xml",
                "classpath*:/plugin-inf/*.xml",
            }, false);

            context.getEnvironment().setActiveProfiles(ReplyTS.EMBEDDED_PROFILE);

            Properties properties = new Properties();

            properties.put("confDir", "classpath:" + configResourcePrefix);

            ClassPathResource resource = new ClassPathResource(configResourcePrefix + "/replyts.properties");

            properties.load(resource.getInputStream());

            properties.put("persistence.cassandra.endpoint", "localhost:" + EmbeddedCassandraServerHelper.getNativeTransportPort());
            properties.put("replyts.http.port", String.valueOf(httpPort));
            properties.put("replyts.ssl.enabled", "false");
            properties.put("delivery.smtp.port", String.valueOf(smtpOutPort));

            properties.put("mailreceiver.filesystem.dropfolder", dropFolder.getAbsolutePath());

            properties.put("search.es.clustername", elasticClusterName);

            properties.put("node.passive", "true");
            properties.put("cluster.jmx.enabled", "false");

            context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test", properties));

            if (testProperties != null) {
                properties.putAll(testProperties);
            }

            context.registerShutdownHook();

            context.refresh();

            if (context.getBean("started", Boolean.class) != true)
                throw new IllegalStateException("COMaaS did not start up in its entirety");

            searchClient = context.getBean(Client.class);

            if (searchClient == null)
                throw new IllegalStateException("COMaaS did not start up in its entirety");
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

    public Client getSearchClient() {
        return searchClient;
    }

    public File getDropFolder() {
        return dropFolder;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }
}
