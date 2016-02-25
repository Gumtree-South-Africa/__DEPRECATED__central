package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.runtime.EnvironmentSupport;
import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.EmbeddedWebServerBuilder;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.jayway.restassured.RestAssured;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Properties;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SSLTest {

    private static final String DEFAULT_PASSWORD = "replyts";

    // find free port for every run, therefore member and not static
    private final int httpPort = OpenPortFinder.findFreePort();
    private final int httpsPort = OpenPortFinder.findFreePort();

    @Mock
    private EnvironmentSupport environmentSupport;

    private EmbeddedWebserver webserver;
    private final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
    private  EmbeddedWebServerBuilder builder;

    @Before
    public void setUp() {

        builder = new EmbeddedWebServerBuilder(environmentSupport);

        when(environmentSupport.logbackAccessConfigExists()).thenReturn(false);

        context.refresh();
        builder.withContext(context)
                .withHttpPort(httpPort)
                .withXmlConfig("classpath:integration-test-replyts-control-context.xml");
    }

    @After
    public void tearDown() throws Exception {
        webserver.shutdown();
        context.stop();
    }

    @Test
    public void runReplyTsWithKeyStoreBasedSSLConfig() throws Exception {
        //Set environment
        Properties properties = new Properties();
        properties.put("replyts.ssl.port", String.valueOf(httpsPort));
        properties.put("replyts.ssl.enabled", "true");
        properties.put("replyts.ssl.store.format", "keystore");
        properties.put("replyts.truststore.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.ts"));
        properties.put("replyts.truststore.password", DEFAULT_PASSWORD);
        properties.put("replyts.keystore.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.jks"));
        properties.put("replyts.keystore.password", DEFAULT_PASSWORD);

        webserver = builder.withProperties(properties).build();

        //SendHttps message
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .get("https://localhost:" + httpsPort);
    }

    @Test
    public void runReplyTsWithPemBasedSSLConfig() throws Exception {
        //Set environment
        Properties properties = new Properties();
        properties.put("replyts.ssl.port", String.valueOf(httpsPort));
        properties.put("replyts.ssl.enabled", "true");
        properties.put("replyts.ssl.store.format", "pem");
        properties.put("replyts.pem.key.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.key"));
        properties.put("replyts.pem.crt.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.crt"));
        properties.put("replyts.pem.password", DEFAULT_PASSWORD);

        webserver = builder.withProperties(properties).build();

        //SendHttps message
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured
                .expect()
                .statusCode(200)
                .when()
                .request()
                .get("https://localhost:" + httpsPort);
    }

    @Test
    public void runReplyTsWithSSLAndPlainHttp() throws Exception {
        //Set environment
        Properties properties = new Properties();
        properties.put("replyts.ssl.port", String.valueOf(httpsPort));
        properties.put("replyts.ssl.enabled", "true");
        properties.put("replyts.ssl.store.format", "keystore");
        properties.put("replyts.truststore.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.ts"));
        properties.put("replyts.truststore.password", DEFAULT_PASSWORD);
        properties.put("replyts.keystore.location", getAbsoluteResourceFilePath("ssl/replyts-self-signed.jks"));
        properties.put("replyts.keystore.password", DEFAULT_PASSWORD);
        properties.put("replyts.ssl.allow.http", "true");
        webserver = builder.withProperties(properties).build();

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

    private String getAbsoluteResourceFilePath(String relativePath) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResource(relativePath).getFile();
    }
}
