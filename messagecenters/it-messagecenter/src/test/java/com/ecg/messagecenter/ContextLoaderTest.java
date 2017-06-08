package com.ecg.messagecenter;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.convert.Converter;
import com.ecg.messagecenter.chat.Template;
import com.ecg.messagecenter.listeners.PostBoxUpdateListener;
import com.ecg.messagecenter.persistence.PostBoxInitializer;
import com.ecg.messagecenter.cronjobs.RiakSimplePostBoxCleanupCronJob;
import com.ecg.messagecenter.persistence.simple.*;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplytsRunner;
import com.google.common.io.Files;
import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertNotNull;

/**
 * Created by jaludden on 30/05/2017.
 */
public class ContextLoaderTest {


    private static final String ELASTIC_SEARCH_PREFIX = "comaas_integration_test_";

    private final File dropFolder = Files.createTempDir();
    private final Integer httpPort = OpenPortFinder.findFreePort();
    private final Integer smtpOutPort = OpenPortFinder.findFreePort();
    private final String elasticClusterName = ELASTIC_SEARCH_PREFIX + UUID.randomUUID();

    @Test
    public void testLoadContext() throws IOException {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.getEnvironment().setActiveProfiles(ReplyTS.EMBEDDED_PROFILE);
        context.register(MailInterceptor.class);

        Properties properties = new Properties();

        properties.put("confDir", "classpath:" + ReplytsRunner.DEFAULT_CONFIG_RESOURCE_DIRECTORY);

        ClassPathResource resource = new ClassPathResource(ReplytsRunner.DEFAULT_CONFIG_RESOURCE_DIRECTORY + "/replyts.properties");

        properties.load(resource.getInputStream());

        properties.put("persistence.cassandra.endpoint", CassandraIntegrationTestProvisioner.getEndPoint());
        properties.put("replyts.http.port", String.valueOf(httpPort));
        properties.put("replyts.ssl.enabled", "false");
        properties.put("delivery.smtp.port", String.valueOf(smtpOutPort));

        properties.put("mailreceiver.filesystem.dropfolder", dropFolder.getAbsolutePath());

        properties.put("search.es.clustername", elasticClusterName);

        properties.put("node.passive", "true");
        properties.put("cluster.jmx.enabled", "false");

        context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("test", properties));

        Properties testProperties = new Properties() {{
            put("replyts.tenant", "it");
            put("persistence.strategy", "riak");
            put("replyts2.cleanup.postboxes.enabled", "true");
        }};

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

        context.register(RiakSimplePostBoxConfiguration.class, MessageCenterConfiguration.class);
        context.refresh();

        assertNotNull(context.getBean(RiakSimplePostBoxRepository.class));
        assertNotNull(context.getBean(PostBoxUpdateListener.class));
        assertNotNull(context.getBean(PostBoxInitializer.class));
        assertNotNull(context.getBean(SpringContextProvider.class));
        assertNotNull(context.getBean(RiakSimplePostBoxCleanupCronJob.class));
        assertNotNull(context.getBean(Template.class));
        assertNotNull(context.getBean(AbstractPostBoxToJsonConverter.class));
        assertNotNull(context.getBean(AbstractJsonToPostBoxConverter.class));
        assertNotNull(context.getBean(Converter.class));
        assertNotNull(context.getBean(ConflictResolver.class));
        assertNotNull(context.getBean(RiakSimplePostBoxMerger.class));
        assertNotNull(context.getBean(IRiakClient.class));
        assertNotNull(context.getBean(RiakSimplePostBoxConverter.class));
    }

}
