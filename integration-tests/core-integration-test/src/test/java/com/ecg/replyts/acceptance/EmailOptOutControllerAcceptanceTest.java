package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.put;
import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;

public class EmailOptOutControllerAcceptanceTest {

    Properties p = new Properties();

    public EmailOptOutControllerAcceptanceTest() {
        p.setProperty("email.opt.out.enabled", "true");
    }

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(p);

    @Test
    public void turnOnOffEmail() throws Exception {

        // turn on by default
        isOn("1");
        isOn("2");

        // turn on is idempotent
        turnOn("1");
        isOn("1");

        // turn off is working
        turnOff("1");
        isOff("1");
        isOn("2");

        // turn off is idempotent
        turnOff("1");
        isOff("1");
        isOn("2");
    }

    private void turnOn(String userId) {
        put("http://localhost:" + rule.getHttpPort() + format("/screeningv2/email-notifications/%s/turn-on", userId)).then()
                .statusCode(200);
    }

    private void turnOff(String userId) {
        put("http://localhost:" + rule.getHttpPort() + format("/screeningv2/email-notifications/%s/turn-off", userId)).then()
                .statusCode(200);
    }

    private void isOn(String userId) {
        get("http://localhost:" + rule.getHttpPort() + format("/screeningv2/email-notifications/%s", userId)).then()
                .statusCode(200)
                .body("body.emailNotifications", equalTo(true));
    }

    private void isOff(String userId) {
        get("http://localhost:" + rule.getHttpPort() + format("/screeningv2/email-notifications/%s", userId)).then()
                .statusCode(200)
                .body("body.emailNotifications", equalTo(false));
    }
}
