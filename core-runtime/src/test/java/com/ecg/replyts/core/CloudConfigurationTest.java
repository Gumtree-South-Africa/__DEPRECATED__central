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
import org.springframework.cloud.client.discovery.DiscoveryClient;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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

    private static final String PROPERTY_FROM_CONSUL_SERVICES = "search.es.endpoints";
    private static final String PROPERTY_FROM_CONSUL_SERVICES_VALUE = "something-that-would-only-be-in-the-services-props";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Test
    public void testServiceDiscoveryShouldDiscoverSharedServiceWhenItsTenantTagIsNotFound() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", "host9", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
          new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertNotNull("No service discovered", childEnvironment.getProperty("search.es.endpoints"));
        assertEquals("host1:1111", childEnvironment.getProperty("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldOnlyDiscoverServicesWithCorrectTenantTag() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", "host8", 8888, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
          new DefaultServiceInstance("elasticsearch", "host9", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
          new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertNotNull("No service discovered", childEnvironment.getProperty("search.es.endpoints"));
        assertEquals("host8:8888", childEnvironment.getProperty("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverSharedService() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Collections.singletonList(
          new DefaultServiceInstance("elasticsearch", "host1", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertNotNull("No service discovered", childEnvironment.getProperty("search.es.endpoints"));
        assertEquals("host1:9999", childEnvironment.getProperty("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverMultipleServices() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
          new DefaultServiceInstance("elasticsearch", "host2", 2222, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
          new DefaultServiceInstance("elasticsearch", "host3", 3333, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertNotNull("No service discovered", childEnvironment.getProperty("search.es.endpoints"));
        assertEquals("host1:1111,host2:2222,host3:3333", childEnvironment.getProperty("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverMultipleTaggedServices() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
          new DefaultServiceInstance("elasticsearch", "host2", 2222, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
          new DefaultServiceInstance("elasticsearch", "host3", 3333, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
          new DefaultServiceInstance("elasticsearch", "host4", 4444, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
          new DefaultServiceInstance("elasticsearch", "host5", 5555, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
          new DefaultServiceInstance("elasticsearch", "host6", 6666, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertNotNull("No service discovered", childEnvironment.getProperty("search.es.endpoints"));
        assertEquals("host3:3333,host4:4444", childEnvironment.getProperty("search.es.endpoints"));
    }

    @Test
    public void testConfDirPropertyPrecedingOverKV() {
        kvProperties.put(PROPERTY_FROM_CONF_DIR, "some-other-value");

        Environment childEnvironment = childContextEnvironment();

        assertEquals("Property defined in confDir takes precedence", PROPERTY_FROM_CONF_DIR_VALUE, childEnvironment.getProperty(PROPERTY_FROM_CONF_DIR));
    }

    @Test
    public void testKVPropertyPrecedingOverDiscoveredService() {
        // First make sure it gets initialized by way of properties

        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", "host9", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
          new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        // Then override it in the KV properties (should take precendence)

        kvProperties.put(PROPERTY_FROM_CONSUL_SERVICES, "some-other-value");

        Environment childEnvironment = childContextEnvironment();

        assertEquals("Property defined in Consul KV takes precedence", "some-other-value", childEnvironment.getProperty(PROPERTY_FROM_CONSUL_SERVICES));

        // Verify that the 'warn' method was called exactly once (to warn of the duplicate property in Consul KV and its subsequent precedence)

        verify(loggerMock, times(1)).warn(anyString(), anyString(), anyString());
    }

    @Test
    public void testServicesProperty() {
        when(discoveryClient.getInstances(eq("elasticsearch"))).thenReturn(Arrays.asList(
          new DefaultServiceInstance("elasticsearch", PROPERTY_FROM_CONSUL_SERVICES_VALUE, 42, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));

        Environment childEnvironment = childContextEnvironment();

        assertEquals("Property defined in Consul takes precedence", PROPERTY_FROM_CONSUL_SERVICES_VALUE + ":42", childEnvironment.getProperty(PROPERTY_FROM_CONSUL_SERVICES));
    }

    // Mock the statically created Logger so that we can test e.g. LOG.warn() being called

    private Logger loggerMock = mock(Logger.class);

    @Before
    public void mockLogger() throws Exception {
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
        private DiscoveryClient discoveryClient;

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