package com.ecg.replyts.core.api.webapi.commands.payloads;

import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class SearchMessagePayloadTest {
    private ObjectMapper mapper;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
    }

    @Test
    public void messageState_givenOneMessageState_parseIntoArray() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("messageState", "HELD");
        String json = mapper.writeValueAsString(map);

        SearchMessagePayload payload = mapper.readValue(json, SearchMessagePayload.class);
        assertTrue(payload.getMessageStates().contains(MessageRtsState.HELD));
    }

    @Test(expected = JsonMappingException.class)
    public void messageState_givenArrayOfMessageState_parseError() throws Exception {
        HashMap<String, String[]> map = new HashMap<String, String[]>();
        map.put("messageState", new String[] {"HELD", "SENT", "BLOCKED"});
        String json = mapper.writeValueAsString(map);

        SearchMessagePayload payload = mapper.readValue(json, SearchMessagePayload.class);
    }

    @Test
    public void messageStates_givenOneMessageState_parseIntoArray() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("messageStates", "HELD");
        String json = mapper.writeValueAsString(map);

        SearchMessagePayload payload = mapper.readValue(json, SearchMessagePayload.class);
        assertTrue(payload.getMessageStates().contains(MessageRtsState.HELD));
    }

    @Test
    public void messageStates_givenArrayOfMessageState_parseIntoArray() throws Exception {
        HashMap<String, String[]> map = new HashMap<String, String[]>();
        map.put("messageStates", new String[] {"HELD", "SENT", "BLOCKED"});
        String json = mapper.writeValueAsString(map);

        SearchMessagePayload payload = mapper.readValue(json, SearchMessagePayload.class);
        assertTrue(payload.getMessageStates().containsAll(Arrays.asList(MessageRtsState.HELD, MessageRtsState.SENT, MessageRtsState.BLOCKED)));
    }
}