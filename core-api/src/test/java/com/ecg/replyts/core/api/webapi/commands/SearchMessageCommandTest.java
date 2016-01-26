package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchMessageCommandTest {

    @Test
    public void generatesCorrectPayload() throws Exception {
        SearchMessagePayload smp = new SearchMessagePayload();
        smp.setAdId("adid");
        smp.setCount(4);

        String payload = new SearchMessageCommand(smp).jsonPayload().get();

        SearchMessagePayload restoredPayload = JsonObjects.getObjectMapper().readValue(payload, SearchMessagePayload.class);


        assertEquals(smp.getAdId(), restoredPayload.getAdId());
        assertEquals(smp.getCount(), restoredPayload.getCount());
    }
}
