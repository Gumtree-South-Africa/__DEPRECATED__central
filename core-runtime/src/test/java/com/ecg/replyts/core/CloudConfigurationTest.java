package com.ecg.replyts.core;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.consul.config.ConsulPropertySourceLocator;
import org.springframework.cloud.consul.discovery.ConsulLifecycle;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@ContextConfiguration(classes = CloudConfigurationTest.ParentTestContext.class)
@TestPropertySource(properties = {
  "confDir = classpath:conf",
  "tenant = ebayk",
  "spring.cloud.consul.enabled = false" // Override Spring Cloud Consul initialization with mock beans
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@PowerMockIgnore("javax.management.*")
public class CloudConfigurationTest {
    private static final String PROPERTY_FROM_CONF_DIR = "some.conf.dir.prop";
    private static final String PROPERTY_FROM_CONF_DIR_VALUE = "something-that-would-only-be-in-the-confDir-props";

    private static final String PROPERTY_FROM_CONSUL_KV = "some.kv.prop";
    private static final String PROPERTY_FROM_CONSUL_KV_VALUE = "something-that-would-only-be-in-the-kv-props";

    @Test
    public void testConfDirPropertyPrecedingOverKV() {
        kvProperties.put(PROPERTY_FROM_CONF_DIR, "some-other-value");

        Environment childEnvironment = childContextEnvironment();

        assertEquals("Property defined in confDir takes precedence", PROPERTY_FROM_CONF_DIR_VALUE, childEnvironment.getProperty(PROPERTY_FROM_CONF_DIR));
    }

    // Mock the statically created Logger so that we can test e.g. LOG.warn() being called

    private Logger loggerMock = mock(Logger.class);

    @Before
    public void mockLogger() {
        Whitebox.setInternalState(CloudConfiguration.class, loggerMock);
    }

    // Tests emulate the normal initialization of Spring Cloud Consul by setting expected behavior on e.g. discoveryClient
    // in a parent context and then initializing a per-test Annotation-based ApplicationContext. The below boilerplate sets
    // up the expected PropertySources (confDir, Consul KV, etc.) located within this parent context

    @Autowired
    private ApplicationContext parentContext;

    @Autowired
    private PropertySourceLocator propertySourceLocator;

    private AnnotationConfigApplicationContext childContext;

    private Map<String, Object> kvProperties = new HashMap<String, Object>() {{
        put(PROPERTY_FROM_CONSUL_KV, PROPERTY_FROM_CONSUL_KV_VALUE);
    }};

    @Before
    public void initialize() {
        PropertySource<?> propertySource = new MapPropertySource("consul", kvProperties);

        when(propertySourceLocator.locate(any())).thenAnswer((Answer<PropertySource<?>>) invocation -> propertySource);
    }

    @After
    public void destroy() {
        childContext.close();
    }

    // This creates the CloudDiscoveryContext itself, wrapped within a parent context. Only the environment is returned
    // as the thing to test are the properties (and which property from which source takes precedence)

    private Environment childContextEnvironment() {
        childContext = new AnnotationConfigApplicationContext();

        childContext.setParent(parentContext);
        childContext.register(CloudConfiguration.class);

        childContext.refresh();

        return childContext.getEnvironment();
    }

    @Configuration
    static class ParentTestContext {
        @MockBean
        private ConsulLifecycle lifecycle;

        @MockBean
        private ConsulPropertySourceLocator propertySourceLocator;

        @MockBean
        private LoggingService loggingService;

        @Autowired
        private ConfigurableEnvironment environment;

        @PostConstruct
        private void initializeMockPropertySource() {
            Map<String, Object> confDirProperties = new HashMap<>();

            confDirProperties.put(PROPERTY_FROM_CONF_DIR, PROPERTY_FROM_CONF_DIR_VALUE);

            environment.getPropertySources().addFirst(new MapPropertySource(ComaasPropertySource.NAME, confDirProperties));
        }
    }
}