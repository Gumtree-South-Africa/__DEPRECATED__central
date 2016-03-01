package com.ecg.de.kleinanzeigen.messagestate;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author mhuttar
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageToEventNameTest {

    @Mock
    private Message message;

    private MessageToEventName namer = new MessageToEventName();

    @Before
    public void setUp() throws Exception {
        when(message.getFilterResultState()).thenReturn(FilterResultState.OK);
        when(message.getHumanResultState()).thenReturn(ModerationResultState.UNCHECKED);
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getLastEditor()).thenReturn(Optional.<String>absent());
    }

    @Test
    public void returnsEndStateIfMessageNotModerated() throws Exception {
        assertEquals("SENT", namer.describeState(message));
    }

    @Test
    public void returnsTraversionToSentAfterModerationToGood() throws Exception {
        when(message.getHumanResultState()).thenReturn(ModerationResultState.GOOD);
        when(message.getFilterResultState()).thenReturn(FilterResultState.HELD);

        assertEquals("FROM_HELD_TO_SENT", namer.describeState(message));

    }

    @Test
    public void returnsTraversionToBlockedAfterModertionToBad() throws Exception {
        when(message.getHumanResultState()).thenReturn(ModerationResultState.BAD);
        when(message.getFilterResultState()).thenReturn(FilterResultState.HELD);

        assertEquals("FROM_HELD_TO_BLOCKED", namer.describeState(message));
    }

    @Test
    public void returnsValidSingleJsonLine() {
        when(message.getHumanResultState()).thenReturn(ModerationResultState.BAD);
        when(message.getFilterResultState()).thenReturn(FilterResultState.HELD);
        when(message.getLastEditor()).thenReturn(Optional.<String>of("foo"));

        assertEquals("{\"event\":\"FROM_HELD_TO_BLOCKED\",\"agent\":\"foo\"}", namer.jsonLogEntry(message));
    }


    @Test
    public void returnsValidSingleJsonLineWhenNoEditor() {

        assertEquals("{\"event\":\"SENT\",\"agent\":null}", namer.jsonLogEntry(message));
    }
}
