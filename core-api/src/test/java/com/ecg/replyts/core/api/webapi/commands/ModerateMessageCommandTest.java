package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModerateMessageCommandTest {


    @Test
    public void buildsCorrectUrl() throws Exception {
        String url = new ModerateMessageCommand("cid", "mid", ModerationResultState.GOOD).url();
        assertEquals("/message/cid/mid/state", url);
    }

    @Test
    public void generatesCorrectPayload() throws Exception {
        ModerateMessageCommand mm = new ModerateMessageCommand("cid", "mid", ModerationResultState.GOOD);

        String payload = mm.jsonPayload().get();
        assertEquals("{\"currentMessageState\":null,\"newMessageState\":\"GOOD\",\"editor\":null}", payload);
    }
}
