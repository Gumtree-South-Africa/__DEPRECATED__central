package com.ecg.replyts.core.runtime.persistence;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationDeletedEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MailsCleanupOnConversationDeletedListenerTest {

    private static final String CONVERSATION_ID = "1234";

    @Mock
    private MailRepository mailRepository;
    @Mock
    private ImmutableConversation conversation;

    @Mock
    private Message message1;

    @Mock
    private Message message2;

    private MailsCleanupOnConversationDeletedListener logic;

    @Before
    public void setUp() {
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(message1.getId()).thenReturn("msg1");
        when(message2.getId()).thenReturn("msg2");
        when(conversation.getMessages()).thenReturn(Arrays.asList(message1, message2));
        logic = new MailsCleanupOnConversationDeletedListener(mailRepository);
    }

    @Test
    public void deletesMailsForConversation() {
        conversationDeletedEvent();

        verify(mailRepository).deleteMail("msg1");
        verify(mailRepository).deleteMail("msg2");
    }

    @Test
    public void skipsErrorOnConversationDeletionWithoutException() {
        doThrow(new RuntimeException()).when(mailRepository).deleteMail("msg1");

        conversationDeletedEvent();
    }

    private void conversationDeletedEvent() {
        logic.eventsTriggered(conversation, Collections.singletonList(new ConversationDeletedEvent(new DateTime())));
    }

}
