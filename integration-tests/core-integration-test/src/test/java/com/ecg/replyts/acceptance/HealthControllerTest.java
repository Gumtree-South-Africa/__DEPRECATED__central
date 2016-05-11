package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.jayway.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.equalTo;

import javax.annotation.PostConstruct;
import java.util.Properties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration(classes = HealthControllerTest.TestConfiguration.class),
    @ContextConfiguration(locations = "classpath:server-context.xml")
})
@ActiveProfiles(ReplyTS.EMBEDDED_PROFILE)
public class HealthControllerTest {
    @Value("${replyts.http.port}")
    private Integer httpPort;

    @Test
    public void checkHealthEndpoint() throws Exception {
        RestAssured
                .expect()
                .statusCode(200)
                .body("version", equalTo(getClass().getPackage().getImplementationVersion()))
                .when()
                .request()
                .get("http://localhost:" + httpPort + "/health");
    }

    @Configuration
    public static class TestConfiguration {
        private static final String DEFAULT_PASSWORD = "replyts";

        @Autowired
        private ConfigurableEnvironment environment;

        @PostConstruct
        public void properties() {
            ClassLoader classLoader = getClass().getClassLoader();

            Properties properties = new Properties();

            properties.put("confDir", "classpath:/integrationtest-conf");
            properties.put("replyts.control.context", "integration-test-control-context.xml");

            properties.put("replyts.http.port", OpenPortFinder.findFreePort());

            environment.getPropertySources().addLast(new PropertiesPropertySource("test", properties));
        }

        // Required by ReplyTS because we are not initializing runtime-context.xml

        @Bean
        public Boolean defaultContextsInitialized() {
            return true;
        }
    }
}
