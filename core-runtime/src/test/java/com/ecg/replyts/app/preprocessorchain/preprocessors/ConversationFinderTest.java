package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversationFinderTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private NewConversationCreator newConversationCreator;

    @Mock
    private ExistingConversationLoader existingConversationLoader;

    @Mock
    private Guids guids;

    @Mock
    private MutableMail mail;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MutableConversation mutableConversation ;

    @Mock
    private ConversationResumer conversationResumer;

    private ConversationFinder conversationFinder;

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Before
    public void setUp() throws Exception {
        conversationFinder = new ConversationFinder(newConversationCreator, existingConversationLoader, new String[]{"ebay.com"}, conversationRepository, conversationResumer);
    }

    @Test
    public void refusesMailsFromCloakedSender() {
        Mockito.when(mail.getFrom()).thenReturn("buyer.123@ebay.com");
        Mockito.when(mail.getDeliveredTo()).thenReturn("somebody@ebay.com");

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, "1", processingTimeGuard);
        conversationFinder.preProcess(messageProcessingContext);

        assertTrue(messageProcessingContext.isTerminated());
    }

    @Test
    public void ifConversationResumableDoNotStartNewConversation() {

        when(conversationResumer.resumeExistingConversation(any(ConversationRepository.class), any(MessageProcessingContext.class))).thenReturn(true);

        when(mail.getFrom()).thenReturn("buyer@host.com");
        when(mail.getDeliveredTo()).thenReturn("somebody@host2.com");
        when(mail.getLastReferencedMessageId()).thenReturn(Optional.empty());

        MessageProcessingContext messageProcessingContext = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));
        when(mutableConversation.getImmutableConversation()).thenReturn(mock(ImmutableConversation.class));
        messageProcessingContext.setConversation(mutableConversation);
        when(messageProcessingContext.getConversation().getId()).thenReturn("123");
        when(messageProcessingContext.getMessageId()).thenReturn("234");
        when(messageProcessingContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);


        conversationFinder.preProcess(messageProcessingContext);

        verify(conversationResumer).resumeExistingConversation(conversationRepository, messageProcessingContext);
        verify(newConversationCreator, never()).setupNewConversation(messageProcessingContext);
    }

    @Test
    public void doesNotAddMessageWhenCannotLoadExistingConversation() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                MessageProcessingContext context = (MessageProcessingContext) invocation.getArguments()[0];
                context.terminateProcessing(MessageState.ORPHANED, this, "");
                return null;
            }
        }).when(existingConversationLoader).loadExistingConversation(any(MessageProcessingContext.class));

        Mockito.when(mail.getFrom()).thenReturn("buyer.123@host.com");
        Mockito.when(mail.getDeliveredTo()).thenReturn("somebody@ebay.com");

        MessageProcessingContext messageProcessingContext = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));
        conversationFinder.preProcess(messageProcessingContext);

        verify(messageProcessingContext, never()).addCommand(any(AddMessageCommand.class));
    }

    @Test
    public void createsNewConversationWhenNotReply() {
        when(mail.getFrom()).thenReturn("buyer.123@host.com");
        when(mail.getDeliveredTo()).thenReturn("somebody@host2.com");
        when(mail.getLastReferencedMessageId()).thenReturn(Optional.empty());
        when(conversationResumer.resumeExistingConversation(any(ConversationRepository.class), any(MessageProcessingContext.class))).thenReturn(false);

        MessageProcessingContext messageProcessingContext = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));
        when(mutableConversation.getImmutableConversation()).thenReturn(mock(ImmutableConversation.class));
        messageProcessingContext.setConversation(mutableConversation);
        when(messageProcessingContext.getConversation().getId()).thenReturn("123");
        when(messageProcessingContext.getMessageId()).thenReturn("234");
        when(messageProcessingContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        conversationFinder.preProcess(messageProcessingContext);

        verify(newConversationCreator).setupNewConversation(messageProcessingContext);
        verify(messageProcessingContext).addCommand(any(AddMessageCommand.class));
    }

    @Test
    public void addsMessageWhenValidReply() {
        when(guids.nextGuid()).thenReturn("aaa");

        when(mail.getFrom()).thenReturn("buyer.123@host.com");
        when(mail.getDeliveredTo()).thenReturn("somebody@ebay.com");
        when(mail.getLastReferencedMessageId()).thenReturn(Optional.empty());

        MessageProcessingContext messageProcessingContext = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));

        messageProcessingContext.setConversation(mutableConversation);
        when(mutableConversation.getImmutableConversation()).thenReturn(mock(ImmutableConversation.class));
        when(messageProcessingContext.getMessageId()).thenReturn("as");
        when(messageProcessingContext.getConversation().getId()).thenReturn("123");
        when(messageProcessingContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        conversationFinder.preProcess(messageProcessingContext);

        verify(messageProcessingContext).addCommand(any(AddMessageCommand.class));
        verify(newConversationCreator, never()).setupNewConversation(any(MessageProcessingContext.class));
    }
}