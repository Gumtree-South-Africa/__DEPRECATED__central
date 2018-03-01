package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.LoggingService;
import com.ecg.replyts.core.Webserver;
import com.ecg.replyts.core.runtime.HttpServerFactory;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.ecg.replyts.core.webapi.control.HealthController;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.jayway.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.ecg.replyts.core.Application.EMBEDDED_PROFILE;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.setEnv;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextHierarchy({
  @ContextConfiguration(classes = HealthControllerTest.CloudConfiguration.class),
  @ContextConfiguration(classes = HealthControllerTest.TestConfiguration.class)
})
@ActiveProfiles(EMBEDDED_PROFILE)
public class HealthControllerTest {
    @Value("#{environment.COMAAS_HTTP_PORT}")
    private Integer httpPort;

    @Test
    public void checkHealthEndpoint() throws Exception {
        RestAssured
          .expect()
          .statusCode(200)
          .body("version", equalTo(HealthController.class.getPackage().getImplementationVersion()))
          .body("cassandraHosts[0]", equalTo("fuzzy-cats"))
          .body("hostname", not(equalTo(null)))
          .when()
          .request()
          .get("http://localhost:" + httpPort + "/health");
    }

    @Configuration
    @Import({ LoggingService.class, Webserver.class, HttpServerFactory.class })
    public static class TestConfiguration {
        @Autowired
        private ConfigurableEnvironment environment;

        @PostConstruct
        public void properties() throws IOException {
            Properties properties = new Properties();

            ClassPathResource resource = new ClassPathResource("/integrationtest-conf/replyts.properties");

            properties.load(resource.getInputStream());

            properties.put("replyts.tenant", "doesntmatter");
            properties.put("hazelcast.password", "123");
            properties.put("hazelcast.port.increment", "true");

            setEnv("COMAAS_HTTP_PORT", String.valueOf(OpenPortFinder.findFreePort()));

            // Disabling both will nonetheless make HealthController 'choose' Cassandra

            properties.put("persistence.strategy", "none");

            properties.put("service.discovery.enabled", true);

            environment.getPropertySources().addLast(new PropertiesPropertySource("test", properties));
        }

        @Bean
        public SpringContextProvider mainContextProvider(ApplicationContext context) {
            return new SpringContextProvider("/", new String[] { "classpath:integration-test-control-context.xml" }, context);
        }
    }

    @Configuration
    @PropertySource("discovery.properties")
    @EnableDiscoveryClient
    @EnableAutoConfiguration(exclude = BusAutoConfiguration.class)
    public static class CloudConfiguration {
        @Autowired
        private ConfigurableEnvironment environment;

        @Autowired
        private DiscoveryClient discoveryClient;

        @PostConstruct
        @ConditionalOnBean(DiscoveryClient.class)
        private void autoDiscoveryOverrides() {
            Map<String, Object> gatheredProperties = new HashMap<>();

            assertNotNull(discoveryClient);

            // Skip actual auto-discovery - @ConditionalOnBean will ensure that this is only called if the DiscoveryClient is available

            gatheredProperties.put("persistence.cassandra.core.endpoint", "fuzzy-cats");

            environment.getPropertySources().addFirst(new MapPropertySource("Auto-discovered services", gatheredProperties));
        }
    }
}
