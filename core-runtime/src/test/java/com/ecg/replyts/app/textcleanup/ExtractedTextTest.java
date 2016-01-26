package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractedTextTest {
    @Mock
    private Conversation conversation;

    @Mock
    private Message message1;


    @Mock
    private PreviousMessageFinder finder;

    @Mock
    private Message prevMessage;

    private ExtractedText text;

    @Before
    public void setup() {
        text = new ExtractedText(message1, finder);
        when(finder.previousMessage(any(Message.class), any(Conversation.class))).thenReturn(Optional.of(prevMessage));
    }

    @Test
    public void returnsFullMailBodyWhenNoPreviousMessageAvailable() {
        when(finder.previousMessage(any(Message.class), any(Conversation.class))).thenReturn(Optional.<Message>absent());
        when(message1.getPlainTextBody()).thenReturn("the full plain text");
        assertEquals("the full plain text", text.in(conversation));
    }

    @Test
    public void returnsDiffFromTwoMessages() throws Exception {
        when(message1.getPlainTextBody()).thenReturn("old text new text");
        when(prevMessage.getPlainTextBody()).thenReturn("old text");
        assertEquals("new text", text.in(conversation));
    }

    @Test
    public void testCompactSpaces() throws Exception {
        when(message1.getPlainTextBody()).thenReturn("hello gumtree london world");
        when(prevMessage.getPlainTextBody()).thenReturn("gumtree london");
        assertEquals("hello world", text.in(conversation));
    }
}
