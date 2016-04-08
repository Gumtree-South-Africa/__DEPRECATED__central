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
    @ContextConfiguration(classes = SSLPlainHTTPTest.TestConfiguration.class),
    @ContextConfiguration(locations = "classpath:server-context.xml")
})
@ActiveProfiles(ReplyTS.EMBEDDED_PROFILE)
public class SSLPlainHTTPTest {
    @Value("${replyts.http.port}")
    private Integer httpPort;
    @Value("${replyts.ssl.port}")
    private Integer httpsPort;

    @Test
    public void runReplyTsWithSSLAndPlainHttp() throws Exception {
        //SendHttps message
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .get("https://localhost:" + httpsPort);

        //Send http request
        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .get("http://localhost:" + httpPort);
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
            properties.put("replyts.ssl.port", OpenPortFinder.findFreePort());
            properties.put("replyts.ssl.enabled", true);
            properties.put("replyts.ssl.store.format", "keystore");
            properties.put("replyts.truststore.location", classLoader.getResource("ssl/replyts-self-signed.ts").getFile());
            properties.put("replyts.truststore.password", DEFAULT_PASSWORD);
            properties.put("replyts.keystore.location", classLoader.getResource("ssl/replyts-self-signed.jks").getFile());
            properties.put("replyts.keystore.password", DEFAULT_PASSWORD);
            properties.put("replyts.ssl.allow.http", true);

            environment.getPropertySources().addLast(new PropertiesPropertySource("test", properties));
        }

        // Required by ReplyTS because we are not initializing runtime-context.xml

        @Bean
        public Boolean defaultContextsInitialized() {
            return true;
        }
    }
}
