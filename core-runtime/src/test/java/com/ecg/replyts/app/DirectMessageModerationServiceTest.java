package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(DirectMessageModerationService.class)
public class DirectMessageModerationServiceTest {
    @MockBean
    private MutableConversationRepository conversationRepository;

    @MockBean
    private ProcessingFlow flow;

    @MockBean
    private SearchIndexer searchIndexer;

    @MockBean
    private HeldMailRepository heldMailRepository;

    @MockBean
    private DefaultMutableConversation c;

    @MockBean
    private Message m;

    @MockBean
    private MessageProcessedListener listener;

    @MockBean
    private ConversationEventListeners conversationEventListeners;

    @MockBean
    private ProcessingContextFactory processingContextFactory;

    @Autowired
    private DirectMessageModerationService mms;


    private byte[] INBOUND_MAIL;

    @Before
    public void setUp() throws MessageNotFoundException, ParsingException {
        when(conversationRepository.getById("1")).thenReturn(c);
        INBOUND_MAIL = "From: foo\nDelivered-To: bar\n\nhello".getBytes();
        when(heldMailRepository.read("1")).thenReturn(INBOUND_MAIL);
        when(processingContextFactory.newContext((Mail) notNull(), eq("1"), (ProcessingTimeGuard) notNull()))
                .thenReturn(new MessageProcessingContext(Mails.readMail(INBOUND_MAIL), "1", new ProcessingTimeGuard(300L)));

        when(c.getId()).thenReturn("1");
        when(c.getMessageById("1")).thenReturn(m);
        when(m.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
    }

    @Test
    public void moderateSendable() throws MessageNotFoundException {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.GOOD, Optional.empty()));

        verify(flow).inputForPostProcessor(any(MessageProcessingContext.class));
        verify(searchIndexer).updateSearchAsync(Arrays.<Conversation>asList(c));
        verify(c).commit(conversationRepository, conversationEventListeners);
    }

    @Test
    public void updatesMailAndIndexOnNotSendable() throws MessageNotFoundException {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.BAD, Optional.empty()));

        verify(c).commit(conversationRepository, conversationEventListeners);
        verify(searchIndexer).updateSearchAsync(Arrays.<Conversation>asList(c));
        verifyZeroInteractions(flow);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectIllegalState() throws MessageNotFoundException {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.UNCHECKED, Optional.empty()));
    }

    @Test
    public void informsListenersAfterCompletion() throws MessageNotFoundException {
        mms.changeMessageState(c, "1", new ModerationAction(ModerationResultState.BAD, Optional.empty()));
        verify(listener).messageProcessed(c, m);
    }
}
