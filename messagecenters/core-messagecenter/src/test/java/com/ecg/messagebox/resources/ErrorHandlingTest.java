package com.ecg.messagebox.resources;

import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationsResource.class)
public class ErrorHandlingTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void internalServerError() throws Exception {
        when(postBoxService.getConversationsById(USER_ID, AD_ID, 500))
                .thenThrow(new RuntimeException("Something happened"));

        JsonNode response = ObjectMapperConfigurer.objectBuilder()
                .put("errorType", "UnknownError")
                .put("errorMessage", "Something happened");

        mvc.perform(get("/users/" + USER_ID + "/ads/" + AD_ID + "/conversations/ids"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(response.toString()));
    }

    @Test
    public void validationError() throws Exception {
        ArrayNode errors = ObjectMapperConfigurer.arrayBuilder()
                .add("System message Ad ID cannot be empty");

        JsonNode response = ObjectMapperConfigurer.objectBuilder()
                .put("errorType", "ValidationError")
                .put("errorMessage", "Validation failed. 1 error(s)")
                .set("errors", errors);

        ObjectNode payload = ObjectMapperConfigurer.objectBuilder()
                .put("text", MESSAGE_TEXT)
                .put("customData", CUSTOM_DATA);

        mvc.perform(post("/users/" + USER_ID + "/conversations/" + CONVERSATION_ID + "/system-messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(response.toString()));
    }

    @Test
    public void noPayload() throws Exception {
        mvc.perform(post("/users/" + USER_ID + "/conversations/" + CONVERSATION_ID + "/system-messages")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
