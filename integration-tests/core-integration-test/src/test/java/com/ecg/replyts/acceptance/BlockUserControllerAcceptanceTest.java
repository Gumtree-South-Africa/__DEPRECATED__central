package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

public class BlockUserControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void blockUser() {
        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void blockSameUserTwice() {
        RestAssured.post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");

        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }


    @Test
    public void unblockUser() {
        RestAssured.post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");

        RestAssured
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void unblockNotBlockedUser() {
        RestAssured
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void userIsNotBlocked() {
        RestAssured
                .expect()
                .content(Matchers.is("false"))
                .statusCode(200)
                .get("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void userIsBlocked() {
        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u11/u22");

        RestAssured
                .expect()
                .content(Matchers.is("true"))
                .statusCode(200)
                .get("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u11/u22");

        RestAssured
                .expect()
                .content(Matchers.is("false"))
                .statusCode(200)
                .get("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u22/u11");
    }

    @Test
    public void listBlockedUsers() {
        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u111/u222");

        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u111/u223");

        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u111/u224");

        RestAssured
                .expect()
                .content(Matchers.is("[ \"u222\", \"u223\", \"u224\" ]"))
                .statusCode(200)
                .get("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u111");
    }
}
