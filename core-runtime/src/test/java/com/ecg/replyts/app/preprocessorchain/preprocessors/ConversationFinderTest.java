package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConversationFinderTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private NewConversationCreator newConversationCreator;

    @Mock
    private ExistingEmailConversationLoader existingEmailConversationLoader;

    @Mock
    private MutableMail mail;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MutableConversation mutableConversation ;

    @Mock
    private ConversationResumer conversationResumer;

    @Captor
    private ArgumentCaptor<AddMessageCommand> addMessageCommandCapture;

    private ConversationFinder conversationFinder;

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Before
    public void setUp() throws Exception {
        conversationFinder = new ConversationFinder(newConversationCreator, existingEmailConversationLoader, new String[]{"ebay.com"}, conversationRepository, conversationResumer);
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
        }).when(existingEmailConversationLoader).loadExistingConversation(any(MessageProcessingContext.class));

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

    @Test
    public void addMessageToReply_shouldAddInResponseToMessageId() {
        when(mail.getFrom()).thenReturn("buyer.123@host.com");
        when(mail.getDeliveredTo()).thenReturn("somebody@ebay.com");
        when(mail.getLastReferencedMessageId()).thenReturn(Optional.of("1:1"));

        MessageProcessingContext messageProcessingContext = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));

        messageProcessingContext.setConversation(mutableConversation);
        when(mutableConversation.getImmutableConversation()).thenReturn(mutableConversation);
        when(mutableConversation.getId()).thenReturn("123");
        when(messageProcessingContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        Message previousMessage = mock(Message.class);
        when(previousMessage.getId()).thenReturn("1:1");
        when(mutableConversation.getMessages()).thenReturn(Arrays.asList(previousMessage));
        conversationFinder.preProcess(messageProcessingContext);

        verify(messageProcessingContext).addCommand(addMessageCommandCapture.capture());
        verify(newConversationCreator, never()).setupNewConversation(any(MessageProcessingContext.class));

        AddMessageCommand addMessageCommand = addMessageCommandCapture.getValue();
        assertThat(addMessageCommand.getInResponseToMessageId()).isEqualTo("1:1");
    }
}