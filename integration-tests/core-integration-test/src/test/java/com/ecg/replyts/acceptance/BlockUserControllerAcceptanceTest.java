package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.jayway.restassured.RestAssured;
import org.junit.Rule;
import org.junit.Test;

public class BlockUserControllerAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void blockUser() throws Exception {
        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void blockSameUserTwice() throws Exception {
        RestAssured.post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");

        RestAssured
                .expect()
                .statusCode(200)
                .post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }


    @Test
    public void unblockUser() throws Exception {
        RestAssured.post("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");

        RestAssured
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }

    @Test
    public void unblockNotBlockedUser() throws Exception {
        RestAssured
                .expect()
                .statusCode(200)
                .delete("http://localhost:" + rule.getHttpPort() + "/screeningv2/block-users/u54231/u12562");
    }
}
