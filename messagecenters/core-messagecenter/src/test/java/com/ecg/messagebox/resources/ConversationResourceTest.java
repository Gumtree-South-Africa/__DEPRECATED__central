package com.ecg.messagebox.resources;

import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationResource.class)
public class ConversationResourceTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void getConversation() throws Exception {
        when(postBoxService.getConversation(USER_BUYER_ID, CONVERSATION_ID, null, 500))
                .thenReturn(Optional.of(conversationThread()));

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/" + CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id", is(CONVERSATION_ID)))
                .andExpect(jsonPath("adId", is(AD_ID)))
                .andExpect(jsonPath("visibility", is("active")))
                .andExpect(jsonPath("messageNotification", is("receive")))
                .andExpect(jsonPath("creationDate", anything()))
                .andExpect(jsonPath("emailSubject", is("SUBJECT")))
                .andExpect(jsonPath("title", is("TITLE")))
                .andExpect(jsonPath("imageUrl", is("IMAGE")))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.latestMessage.id", anything()))
                .andExpect(jsonPath("$.latestMessage.type", is("chat")))
                .andExpect(jsonPath("$.latestMessage.text", is("MESSAGE TEXT")))
                .andExpect(jsonPath("$.latestMessage.senderUserId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("$.latestMessage.receivedDate", anything()))
                .andExpect(jsonPath("$.latestMessage.isRead", is(true)))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[0].userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("$.participants[0].name", is("BUYER_NAME")))
                .andExpect(jsonPath("$.participants[0].email", is("BUYER_EMAIL")))
                .andExpect(jsonPath("$.participants[0].role", is("buyer")))
                .andExpect(jsonPath("$.participants[1].userId", is(USER_SELLER_ID)))
                .andExpect(jsonPath("$.participants[1].name", is("SELLER_NAME")))
                .andExpect(jsonPath("$.participants[1].email", is("SELLER_EMAIL")))
                .andExpect(jsonPath("$.participants[1].role", is("seller")));
    }

    @Test
    public void conversationNotFound() throws Exception {
        when(postBoxService.getConversation(USER_BUYER_ID, "NOT_EXIST", null, 500))
                .thenReturn(Optional.empty());

        JsonNode response = ObjectMapperConfigurer.objectBuilder()
                .put("errorType", "EntityNotFound")
                .put("errorMessage", "Conversation not found for ID: NOT_EXIST");

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/NOT_EXIST")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(response.toString()));
    }

    @Test
    public void readConversation() throws Exception {
        when(postBoxService.markConversationAsRead(USER_BUYER_ID, CONVERSATION_ID, null, 500))
                .thenReturn(Optional.of(conversationThread()));

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/" + CONVERSATION_ID + "/read")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("id", is(CONVERSATION_ID)))
                .andExpect(jsonPath("adId", is(AD_ID)))
                .andExpect(jsonPath("visibility", is("active")))
                .andExpect(jsonPath("messageNotification", is("receive")))
                .andExpect(jsonPath("creationDate", anything()))
                .andExpect(jsonPath("emailSubject", is("SUBJECT")))
                .andExpect(jsonPath("title", is("TITLE")))
                .andExpect(jsonPath("imageUrl", is("IMAGE")))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.latestMessage.id", anything()))
                .andExpect(jsonPath("$.latestMessage.type", is("chat")))
                .andExpect(jsonPath("$.latestMessage.text", is("MESSAGE TEXT")))
                .andExpect(jsonPath("$.latestMessage.senderUserId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("$.latestMessage.receivedDate", anything()))
                .andExpect(jsonPath("$.latestMessage.isRead", is(true)))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[0].userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("$.participants[0].name", is("BUYER_NAME")))
                .andExpect(jsonPath("$.participants[0].email", is("BUYER_EMAIL")))
                .andExpect(jsonPath("$.participants[0].role", is("buyer")))
                .andExpect(jsonPath("$.participants[1].userId", is(USER_SELLER_ID)))
                .andExpect(jsonPath("$.participants[1].name", is("SELLER_NAME")))
                .andExpect(jsonPath("$.participants[1].email", is("SELLER_EMAIL")))
                .andExpect(jsonPath("$.participants[1].role", is("seller")));
    }

    @Test
    public void readConversationNotFound() throws Exception {
        when(postBoxService.markConversationAsRead(USER_BUYER_ID, "NOT_EXIST", null, 500))
                .thenReturn(Optional.empty());

        JsonNode response = ObjectMapperConfigurer.objectBuilder()
                .put("errorType", "EntityNotFound")
                .put("errorMessage", "Conversation not found for ID: NOT_EXIST");

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/NOT_EXIST/read")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(response.toString()));
    }

    @Test
    public void postSystemMessage() throws Exception {
        ObjectNode payload = ObjectMapperConfigurer.objectBuilder()
                .put("adId", AD_ID)
                .put("text", MESSAGE_TEXT)
                .put("customData", CUSTOM_DATA);

        mvc.perform(post("/users/" + USER_ID + "/conversations/" + CONVERSATION_ID + "/system-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.toString()))
                .andExpect(status().isCreated());

        verify(postBoxService).createSystemMessage(USER_ID, CONVERSATION_ID, AD_ID, MESSAGE_TEXT, CUSTOM_DATA, false);
    }
}
