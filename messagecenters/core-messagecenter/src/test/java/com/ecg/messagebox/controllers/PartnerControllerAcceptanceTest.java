package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.requests.PartnerMessagePayload;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.jayway.restassured.RestAssured;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.Matchers.equalTo;

public class PartnerControllerAcceptanceTest extends ReplyTsIntegrationTestRuleHelper {

    private JerseyWebTarget partnerWebTarget = null;

    @Before
    public void setup() {
        partnerWebTarget = JerseyClientBuilder.createClient()
                .register(new JacksonJaxbJsonProvider())
                .target("http://localhost:" + testRule.getHttpPort() + "/msgcenter/partner-sync");
    }

    @Test
    public void sentPartnerMessage() {
        PartnerMessagePayload payload = new PartnerMessagePayload();
        payload.setAdId("AD_ID");
        payload.setAdTitle("AD_TITLE");
        payload.setBuyer(new Participant("PARTNER_BUYER_USER_ID", "PARTNER_BUYER_NAME", "PARTNER_BUYER_EMAIL", ParticipantRole.BUYER));
        payload.setSeller(new Participant("PARTNER_SELLER_USER_ID", "PARTNER_SELLER_NAME", "PARTNER_SELLER_EMAIL", ParticipantRole.SELLER));
        payload.setSenderUserId("PARTNER_SELLER_USER_ID");
        payload.setSubject("SUBJECT");
        payload.setText("MESSAGE TEXT");
        payload.setType(MessageType.PARTNER);

        Response response = call(payload);
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            Assert.fail("Error during posting partner message.");
        }

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.userId", equalTo("PARTNER_BUYER_USER_ID"))
                .body("body.unreadMessagesCount", equalTo(1))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(1))
                .body("body.conversations[0].adId", equalTo("AD_ID"))
                .body("body.conversations[0].visibility", equalTo("active"))
                .body("body.conversations[0].messageNotification", equalTo("receive"))
                .body("body.conversations[0].participants[0].userId", equalTo("PARTNER_BUYER_USER_ID"))
                .body("body.conversations[0].participants[0].name", equalTo("PARTNER_BUYER_NAME"))
                .body("body.conversations[0].participants[0].email", equalTo("PARTNER_BUYER_EMAIL"))
                .body("body.conversations[0].participants[0].role", equalTo("buyer"))
                .body("body.conversations[0].participants[1].userId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.conversations[0].participants[1].name", equalTo("PARTNER_SELLER_NAME"))
                .body("body.conversations[0].participants[1].email", equalTo("PARTNER_SELLER_EMAIL"))
                .body("body.conversations[0].participants[1].role", equalTo("seller"))
                .body("body.conversations[0].latestMessage.type", equalTo("partner"))
                .body("body.conversations[0].latestMessage.text", equalTo("MESSAGE TEXT"))
                .body("body.conversations[0].latestMessage.senderUserId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.conversations[0].latestMessage.isRead", equalTo(true))
                .body("body.conversations[0].emailSubject", equalTo("SUBJECT"))
                .body("body.conversations[0].title", equalTo("AD_TITLE"))
                .body("body.conversations[0].unreadMessagesCount", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/PARTNER_BUYER_USER_ID/conversations/")
                .then().log().all();

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.userId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.unreadMessagesCount", equalTo(0))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(0))
                .body("body.conversations[0].adId", equalTo("AD_ID"))
                .body("body.conversations[0].visibility", equalTo("active"))
                .body("body.conversations[0].messageNotification", equalTo("receive"))
                .body("body.conversations[0].participants[0].userId", equalTo("PARTNER_BUYER_USER_ID"))
                .body("body.conversations[0].participants[0].name", equalTo("PARTNER_BUYER_NAME"))
                .body("body.conversations[0].participants[0].email", equalTo("PARTNER_BUYER_EMAIL"))
                .body("body.conversations[0].participants[0].role", equalTo("buyer"))
                .body("body.conversations[0].participants[1].userId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.conversations[0].participants[1].name", equalTo("PARTNER_SELLER_NAME"))
                .body("body.conversations[0].participants[1].email", equalTo("PARTNER_SELLER_EMAIL"))
                .body("body.conversations[0].participants[1].role", equalTo("seller"))
                .body("body.conversations[0].latestMessage.type", equalTo("partner"))
                .body("body.conversations[0].latestMessage.text", equalTo("MESSAGE TEXT"))
                .body("body.conversations[0].latestMessage.senderUserId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.conversations[0].latestMessage.isRead", equalTo(false))
                .body("body.conversations[0].emailSubject", equalTo("SUBJECT"))
                .body("body.conversations[0].title", equalTo("AD_TITLE"))
                .body("body.conversations[0].unreadMessagesCount", equalTo(0))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/PARTNER_SELLER_USER_ID/conversations/")
                .then().log().all();

        PartnerMessagePayload payload2 = new PartnerMessagePayload();
        payload2.setAdId("AD_ID");
        payload2.setAdTitle("AD_TITLE");
        payload2.setBuyer(new Participant("PARTNER_BUYER_USER_ID", "PARTNER_BUYER_NAME", "PARTNER_BUYER_EMAIL", ParticipantRole.BUYER));
        payload2.setSeller(new Participant("PARTNER_SELLER_USER_ID", "PARTNER_SELLER_NAME", "PARTNER_SELLER_EMAIL", ParticipantRole.SELLER));
        payload2.setSenderUserId("PARTNER_BUYER_USER_ID");
        payload2.setSubject("SUBJECT");
        payload2.setText("REPLY MESSAGE TEXT");
        payload2.setType(MessageType.PARTNER);

        Response response2 = call(payload2);
        if (response2.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            Assert.fail("Error during posting replay partner message.");
        }

        RestAssured.given()
                .expect()
                .statusCode(200)
                .body("body.userId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.unreadMessagesCount", equalTo(1))
                .body("body.conversationsWithUnreadMessagesCount", equalTo(1))
                .body("body.conversations[0].adId", equalTo("AD_ID"))
                .body("body.conversations[0].visibility", equalTo("active"))
                .body("body.conversations[0].messageNotification", equalTo("receive"))
                .body("body.conversations[0].participants[0].userId", equalTo("PARTNER_BUYER_USER_ID"))
                .body("body.conversations[0].participants[0].name", equalTo("PARTNER_BUYER_NAME"))
                .body("body.conversations[0].participants[0].email", equalTo("PARTNER_BUYER_EMAIL"))
                .body("body.conversations[0].participants[0].role", equalTo("buyer"))
                .body("body.conversations[0].participants[1].userId", equalTo("PARTNER_SELLER_USER_ID"))
                .body("body.conversations[0].participants[1].name", equalTo("PARTNER_SELLER_NAME"))
                .body("body.conversations[0].participants[1].email", equalTo("PARTNER_SELLER_EMAIL"))
                .body("body.conversations[0].participants[1].role", equalTo("seller"))
                .body("body.conversations[0].latestMessage.type", equalTo("partner"))
                .body("body.conversations[0].latestMessage.text", equalTo("REPLY MESSAGE TEXT"))
                .body("body.conversations[0].latestMessage.senderUserId", equalTo("PARTNER_BUYER_USER_ID"))
                .body("body.conversations[0].latestMessage.isRead", equalTo(false))
                .body("body.conversations[0].emailSubject", equalTo("SUBJECT"))
                .body("body.conversations[0].title", equalTo("AD_TITLE"))
                .body("body.conversations[0].unreadMessagesCount", equalTo(1))
                .get("http://localhost:" + testRule.getHttpPort() + "/msgcenter/users/PARTNER_SELLER_USER_ID/conversations/")
                .then().log().all();
    }

    private Response call(PartnerMessagePayload payload) {
        return partnerWebTarget
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));
    }
}