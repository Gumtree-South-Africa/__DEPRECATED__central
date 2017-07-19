package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.webapi.control.HealthController;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.jayway.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.ecg.replyts.core.runtime.ReplyTS.EMBEDDED_PROFILE;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.setEnv;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextHierarchy({
        @ContextConfiguration(classes = HealthControllerTest.TestConfiguration.class),
        @ContextConfiguration(classes = HealthControllerTest.DiscoveryConfiguration.class),
        @ContextConfiguration(locations = "classpath:server-context.xml")
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
                .body("discoveryEnabled", equalTo(true))
                .body("cassandraHosts[0]", equalTo("fuzzy-cats"))
                .body("hostname", not(equalTo(null)))
                .when()
                .request()
                .get("http://localhost:" + httpPort + "/health");
    }

    @Configuration
    public static class TestConfiguration {
        @Autowired
        private ConfigurableEnvironment environment;

        @PostConstruct
        public void properties() {
            Properties properties = new Properties();

            properties.put("confDir", "classpath:/integrationtest-conf");
            properties.put("replyts.control.context", "integration-test-control-context.xml");

            setEnv("COMAAS_HTTP_PORT", String.valueOf(OpenPortFinder.findFreePort()));

            // Disabling both will nonetheless make HealthController 'choose' Cassandra

            properties.put("persistence.strategy", "none");

            properties.put("service.discovery.enabled", true);

            environment.getPropertySources().addLast(new PropertiesPropertySource("test", properties));
        }

        // Required by ReplyTS because we are not initializing runtime-context.xml

        @Bean
        public Boolean defaultContextsInitialized() {
            return true;
        }
    }

    @Configuration
    @PropertySource("discovery.properties")
    @EnableDiscoveryClient
    @EnableAutoConfiguration(exclude = {FreeMarkerAutoConfiguration.class, DataSourceAutoConfiguration.class, BusAutoConfiguration.class})
    public static class DiscoveryConfiguration {
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
