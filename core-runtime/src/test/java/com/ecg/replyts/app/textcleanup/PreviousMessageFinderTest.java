package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class PreviousMessageFinderTest {

    @Mock
    private Conversation conversation;

    @Mock
    private Message message1;

    @Mock
    private Message message2;

    @Mock
    private Message message3;

    @Mock
    private Message messageThatIsNotInConversation;

    private PreviousMessageFinder finder = new PreviousMessageFinder();


    @Before
    public void setup() {
        when(conversation.getMessages()).thenReturn(Lists.newArrayList(message1, message2, message3));
        when(message1.getId()).thenReturn("id1");
        when(message2.getId()).thenReturn("id2");
        when(message3.getId()).thenReturn("id3");
        when(conversation.getMessageById("id1")).thenReturn(message1);
        when(conversation.getMessageById("id2")).thenReturn(message2);
        when(conversation.getMessageById("id3")).thenReturn(message3);
    }

    @Test
    public void doesNotFindPreviousMessageForFirstMessageInConversation() {
        assertFalse(finder.previousMessage(message1, conversation).isPresent());
    }

    @Test
    public void findsFirstMessageForSecondMessage() {
        assertEquals(message1, finder.previousMessage(message2, conversation).get());
    }

    @Test
    public void findsReferencedMessage() {
        when(message3.getInResponseToMessageId()).thenReturn("id1");
        assertEquals(message1, finder.previousMessage(message3, conversation).get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMessageIfItIsFromADifferentConversation() {
        finder.previousMessage(messageThatIsNotInConversation, conversation);
    }
}
