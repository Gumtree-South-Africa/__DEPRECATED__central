package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.ecg.replyts.integration.test.filter.ExampleFilterFactory;
import com.ecg.replyts.integration.test.filter.RejectingConfigurationFilterFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItems;

public class ConfigApiAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();


    String newFilterConfig = JsonObjects.builder()
            .attr("state", PluginState.ENABLED.name())
            .attr("priority", 100l)
            .attr("configuration", JsonObjects.builder().attr("foo", "bar"))
            .toJson();
    Header appjson = new Header("Content-Type", "application/json");


    @Test
    public void respondsWithCorrectContentType() {
        RestAssured.expect().header("Content-Type", "application/json;charset=UTF-8")
                .when().get(subPath(""));
    }

    @Test
    public void storesNewConfiguration() {
        RestAssured.expect()
                .statusCode(200)
                .and()
                .body("state", CoreMatchers.is("OK"))

                .when()
                .request()
                .header(appjson)
                .body(newFilterConfig)
                .put(subPath(ExampleFilterFactory.class.getName() + "/testinstance"));
    }


    @Test
    public void storesUpdatedConfiguration() throws Exception {
        RestAssured.expect()
                .statusCode(200)
                .and()
                .body("state", CoreMatchers.is("OK"))

                .when()
                .request()
                .header(appjson)
                .body(newFilterConfig)
                .put(subPath(ExampleFilterFactory.class.getName() + "/testinstance"));


        String configV2 = JsonObjects.builder()
                .attr("state", PluginState.ENABLED.name())
                .attr("priority", 200l)
                .attr("configuration", JsonObjects.builder().attr("foo", "bar"))
                .toJson();

        RestAssured.expect()
                .statusCode(200)
                .and()
                .body("state", CoreMatchers.is("OK"))

                .when()
                .request()
                .header(appjson)
                .body(configV2)
                .put(subPath(ExampleFilterFactory.class.getName() + "/testinstance"));


        RestAssured.expect().body("configs[0].priority", is(200)).when().get(subPath(""));

    }

    @Test
    public void rejectsConfigurationForUnknownPlugin() {
        RestAssured.expect()
                .statusCode(500)
                .and()
                .body("state", is("FAILURE"))

                .when()
                .request()
                .header(appjson)
                .body(newFilterConfig)
                .put(subPath("/com.acme.DoesNotExistImpl/testinstance"));
    }

    @Test
    public void rejectsErroneousConfiguration() {
        RestAssured.expect()
                .statusCode(500)
                .and()
                .body("state", is("FAILURE"))
                .when()
                .request()
                .header(appjson)
                .body(newFilterConfig)
                .put(subPath(RejectingConfigurationFilterFactory.class.getName() + "/instance"));
    }

    @Test
    public void listsExistingConfigurations() throws InterruptedException {
        RestAssured.with().header(appjson).body(newFilterConfig).put(subPath(ExampleFilterFactory.class.getName() + "/newinstance")).andReturn();

        RestAssured.expect().body("configs.instanceId", hasItems("newinstance")).when().get(subPath(""));
    }

    @Test
    public void deletedExistingConfiguration() {
        RestAssured.with().header(appjson).body(newFilterConfig).put(subPath(ExampleFilterFactory.class.getName() + "/todelete")).andReturn();
        RestAssured.expect().statusCode(200).and().body("state", is("OK")).when().delete(subPath(ExampleFilterFactory.class.getName() + "/todelete"));
    }


    private String subPath(String subPath) {
        int rtsHttpPort = testRule.getHttpPort();
        return String.format("http://localhost:%d/configv2/%s", rtsHttpPort, subPath);
    }


}