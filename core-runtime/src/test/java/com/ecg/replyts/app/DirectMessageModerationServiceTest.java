package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DirectMessageModerationServiceTest {

    @Mock
    private MutableConversationRepository conversationRepository;
    @Mock
    private ProcessingFlow flow;
    @Mock
    private SearchIndexer searchIndexer;
    @Mock
    private MailRepository mailRepository;

    @Mock
    private DefaultMutableConversation c;

    @Mock
    private Message m;

    @Mock
    private MessageProcessedListener listener;

    @Mock
    private ConversationEventListeners conversationEventListeners;

    private DirectMessageModerationService mms;
    private byte[] INBOUND_MAIL;

    @Before
    public void setUp() {
        mms = new DirectMessageModerationService(conversationRepository, flow, mailRepository, searchIndexer, asList(listener), conversationEventListeners);
        when(conversationRepository.getById("1")).thenReturn(c);
        INBOUND_MAIL = "From: foo\nDelivered-To: bar\n\nhello".getBytes();
        when(mailRepository.readInboundMail("1")).thenReturn(INBOUND_MAIL);

        when(c.getId()).thenReturn("1");
        when(c.getMessageById("1")).thenReturn(m);
        when(m.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
    }

    @Test
    public void moderateSendable() {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.GOOD, Optional.<String>absent()));

        verify(flow).inputForPostProcessor(any(MessageProcessingContext.class));
        verify(mailRepository).persistMail(eq("1"), any(byte[].class), any(Optional.class));
        verify(searchIndexer).updateSearchAsync(Arrays.<Conversation>asList(c));
        verify(c).commit(conversationRepository, conversationEventListeners);
    }

    @Test
    public void updatesMailAndIndexOnNotSendable() {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.BAD, Optional.<String>absent()));

        verify(c).commit(conversationRepository, conversationEventListeners);
        verify(searchIndexer).updateSearchAsync(Arrays.<Conversation>asList(c));
        verifyZeroInteractions(flow);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectIllegalState() {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.UNCHECKED, Optional.<String>absent()));
    }

    @Test
    public void informsListenersAfterCompletion() {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.BAD, Optional.<String>absent()));
        verify(listener).messageProcessed(c, m);
    }
}
