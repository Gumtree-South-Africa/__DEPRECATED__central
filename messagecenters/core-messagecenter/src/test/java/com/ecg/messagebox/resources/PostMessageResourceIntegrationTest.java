package com.ecg.messagebox.resources;

import com.ecg.messagebox.controllers.requests.PostMessageRequest;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

public class PostMessageResourceIntegrationTest {

    private static int MAX_ATTACHMENT_SIZE_MB = 1;
    private static int MAX_ATTACHMENT_SIZE = MAX_ATTACHMENT_SIZE_MB * 1024 * 1024;
    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(
            createProperties(),
            null,
            20,
            false,
            new Class[]{Object.class},
            "cassandra_schema.cql",
            "cassandra_messagebox_schema.cql",
            "cassandra_messagecenter_schema.cql");

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_MP);
        properties.put("replyts.tenant", TENANT_MP);
        properties.put("jetty.request.max_size_mb", MAX_ATTACHMENT_SIZE_MB);
        return properties;
    }

    @Test
    public void testPostMessage() throws Exception {

        RestAssured
                .given()
                    .contentType("application/json")
                    .body(defaultMessage())
                .expect()
                    .statusCode(200)
                .when()
                    .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1");
    }

    @Test
    public void testEmptyPostMessage() throws Exception {

        RestAssured
                .given()
                    .contentType("application/json")
                .expect()
                    .statusCode(400)
                .when()
                    .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1");
    }

    @Test
    public void testPostMessageAttachWithoutMessage() throws Exception {

        byte[] bytes = new byte[100];

        RestAssured
                .given()
                    .contentType("multipart/form-data")
                    .multiPart("attachment", "value", bytes)
                .expect()
                    .statusCode(400)
                .when()
                    .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1/attachment");

    }

    @Test
    public void testPostMessageAttachWithoutAttachment() throws Exception {

        RestAssured
                .given()
                    .contentType("multipart/form-data")
                    .multiPart("message", defaultMessage(), "application/json")
                .expect()
                .statusCode(400)
                .when()
                    .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1/attachment");
    }

    @Test
    public void testPostMessageAttachExceedingSize() throws Exception {

        // the attachment bigger than the configured size
        byte[] bytes = new byte[MAX_ATTACHMENT_SIZE + 1];

        RestAssured
                .given()
                    .contentType("multipart/form-data")
                    .multiPart("message", defaultMessage(), "application/json")
                    .multiPart("attachment", "value", bytes)
                .expect()
                    .statusCode(500)
                .when()
                    .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1/attachment");

        // the message and the attachment together bigger than the configured size
        bytes = new byte[1 + MAX_ATTACHMENT_SIZE / 2];
        String bigValue = String.join("", Collections.nCopies(MAX_ATTACHMENT_SIZE / 2, "0"));

        RestAssured
                .given()
                .contentType("multipart/form-data")
                .multiPart("message", defaultMessage(bigValue), "application/json")
                .multiPart("attachment", "value", bytes)
                .expect()
                .statusCode(500)
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1/attachment");
    }

    @Test
    public void testPostMessageAttachFullSuccess() throws Exception {

        byte[] bytes = new byte[MAX_ATTACHMENT_SIZE - 1000];

        RestAssured
                .given()
                .contentType("multipart/form-data")
                .multiPart("message", defaultMessage(), "application/json")
                .multiPart("attachment", "value", bytes)
                .expect()
                .statusCode(200)
                .when()
                .post("http://localhost:" + testRule.getHttpPort() + "/msgbox/users/2/conversations/conv1/attachment");
    }

    private static String defaultMessage() throws Exception {
        return defaultMessage("message");
    }

    private static String defaultMessage(String content) throws Exception {
        PostMessageRequest request = new PostMessageRequest();
        request.message = content;

        request.metadata = new HashMap<>();
        request.metadata.put("metadata_key", "metadata_value");

        return new ObjectMapper().writeValueAsString(request);
    }

}

