package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProcessingFinalizerTest {

    @Mock
    private MutableConversationRepository conversationRepository;

    @Mock
    private MailRepository mailRepository;

    @Mock
    private SearchIndexer searchIndexer;


    @Mock
    private DefaultMutableConversation conv;

    @Mock
    private Termination termination;

    @Mock
    private ExcessiveConversationSizeConstraint constraint;

    @Mock
    private ConversationEventListeners conversationEventListeners;

    @Mock
    private MailPublisher mailProcessedListener;

    private ProcessingFinalizer messagePersister;

    @Before
    public void setUp() throws Exception {
        when(constraint.tooManyMessagesIn(any(Conversation.class))).thenReturn(false);
        messagePersister = new ProcessingFinalizer(conversationRepository, mailRepository, searchIndexer, constraint,
                conversationEventListeners, mailProcessedListener, false);
        when(termination.getEndState()).thenReturn(MessageState.ORPHANED);
        when(termination.getIssuer()).thenReturn(Object.class);
        when(conv.getId()).thenReturn("a");
    }


    @Test
    public void alwaysTerminatesMessageWhenCompleted() {
        messagePersister.persistAndIndex(conv, "1", "incoming".getBytes(), Optional.<byte[]>absent(), termination);
        verify(conv).applyCommand(any(MessageTerminatedCommand.class));
    }

    @Test
    public void persistsOutgoingMailIfAvailable() {
        messagePersister.persistAndIndex(conv, "1", "incoming".getBytes(), Optional.of("outgoing".getBytes()), termination);

        verify(mailRepository).persistMail(anyString(), any(byte[].class), any(Optional.class));
    }


    @Test
    public void persistsData() {
        messagePersister.persistAndIndex(conv, "1", "incoming".getBytes(), Optional.of("outgoing".getBytes()), termination);

        verify(conv).commit(conversationRepository, conversationEventListeners);

        verify(mailRepository).persistMail(anyString(), any(byte[].class), any(Optional.class));

        verify(searchIndexer).updateSearchAsync(Arrays.<Conversation>asList(conv));
    }

    @Test
    public void skipsUpdatingIfConversationSizeExceedsConstraint() {
        when(constraint.tooManyMessagesIn(any(Conversation.class))).thenReturn(true);

        messagePersister.persistAndIndex(conv, "1", "incoming".getBytes(), Optional.of("outgoing".getBytes()), termination);

        verify(conv, never()).commit(conversationRepository, conversationEventListeners);

        verify(mailRepository, never()).persistMail(anyString(), any(byte[].class), any(Optional.class));

        verify(searchIndexer, never()).updateSearchAsync(Arrays.<Conversation>asList(conv));
    }
}

