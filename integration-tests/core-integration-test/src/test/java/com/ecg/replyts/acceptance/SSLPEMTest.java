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

import javax.annotation.PostConstruct;
import java.util.Properties;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration(classes = SSLPEMTest.TestConfiguration.class),
    @ContextConfiguration(locations = "classpath:server-context.xml")
})
@ActiveProfiles(ReplyTS.EMBEDDED_PROFILE)
public class SSLPEMTest {
    @Value("${replyts.ssl.port}")
    private int httpsPort;

    @Test
    public void runReplyTsWithPemBasedSSLConfig() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .get("https://localhost:" + httpsPort);
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

            properties.put("replyts.ssl.port", OpenPortFinder.findFreePort());
            properties.put("replyts.ssl.enabled", true);
            properties.put("replyts.ssl.store.format", "pem");
            properties.put("replyts.pem.key.location", classLoader.getResource("ssl/replyts-self-signed.key").getFile());
            properties.put("replyts.pem.crt.location", classLoader.getResource("ssl/replyts-self-signed.crt").getFile());
            properties.put("replyts.pem.password", DEFAULT_PASSWORD);

            environment.getPropertySources().addLast(new PropertiesPropertySource("test", properties));
        }

        // Required by ReplyTS because we are not initializing runtime-context.xml

        @Bean
        public Boolean defaultContextsInitialized() {
            return true;
        }
    }
}
