package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.util.TimeFormatUtils;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResponseDataResource.class)
public class ResponseDataResourceTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void emptyResponseData() throws Exception {
        when(responseDataService.getResponseData(USER_ID))
                .thenReturn(Collections.emptyList());

        mvc.perform(get("/users/" + USER_ID + "/response-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("[]"));
    }

    @Test
    public void oneResponseData() throws Exception {
        DateTime now = DateTime.now();
        ResponseData responseData = new ResponseData(USER_ID, CONVERSATION_ID, now, MessageType.CHAT, 15);

        when(responseDataService.getResponseData(USER_ID))
                .thenReturn(Collections.singletonList(responseData));

        ObjectNode object = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("userId", USER_ID)
                .put("conversationId", CONVERSATION_ID)
                .put("responseSpeed", 15)
                .put("conversationCreationDate", TimeFormatUtils.format(now))
                .put("conversationType", "chat");

        String response = ObjectMapperConfigurer.getObjectMapper()
                .createArrayNode()
                .arrayNode()
                .add(object)
                .toString();

        mvc.perform(get("/users/" + USER_ID + "/response-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(response));
    }

    @Test
    public void multiResponseData() throws Exception {
        DateTime now = DateTime.now();
        ResponseData responseData = new ResponseData(USER_ID, CONVERSATION_ID, now, MessageType.CHAT, 15);
        ResponseData responseData2 = new ResponseData(USER_ID, CONVERSATION_ID, now, MessageType.CHAT);

        when(responseDataService.getResponseData(USER_ID))
                .thenReturn(Arrays.asList(responseData, responseData2));

        ObjectNode object = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("userId", USER_ID)
                .put("conversationId", CONVERSATION_ID)
                .put("responseSpeed", 15)
                .put("conversationCreationDate", TimeFormatUtils.format(now))
                .put("conversationType", "chat");

        ObjectNode object2 = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("userId", USER_ID)
                .put("conversationId", CONVERSATION_ID)
                .put("responseSpeed", -1)
                .put("conversationCreationDate", TimeFormatUtils.format(now))
                .put("conversationType", "chat");

        String response = ObjectMapperConfigurer.getObjectMapper()
                .createArrayNode()
                .arrayNode()
                .add(object)
                .add(object2)
                .toString();

        mvc.perform(get("/users/" + USER_ID + "/response-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(response));
    }

    @Test
    public void aggregatedResponseData() throws Exception {
        AggregatedResponseData responseData = new AggregatedResponseData(1, 2);

        when(responseDataService.getAggregatedResponseData(USER_ID))
                .thenReturn(Optional.of(responseData));

        String response = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("speed", 1)
                .put("rate", 2)
                .toString();

        mvc.perform(get("/users/" + USER_ID + "/aggregated-response-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(response));
    }

    @Test
    public void aggregatedResponseDataNotFound() throws Exception {
        when(responseDataService.getAggregatedResponseData(USER_ID))
                .thenReturn(Optional.empty());

        JsonNode response = ObjectMapperConfigurer.objectBuilder()
                .put("message", "AggregationResponseData not found for userID: USER_ID");

        mvc.perform(get("/users/" + USER_ID + "/aggregated-response-data")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(response.toString()));
    }
}
