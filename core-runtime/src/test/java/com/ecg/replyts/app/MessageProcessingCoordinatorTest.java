package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageProcessingCoordinatorTest {

    @Mock
    private Mails mails;

    @Mock
    private ProcessingFinalizer persister;

    @Mock
    private Guids guids;

    @Mock
    private Mail mail;

    @Mock
    private ProcessingFlow flow;

    @Mock
    private DefaultMutableConversation conversation;

    @Mock
    private MessageProcessedListener messageProcessedListener;

    @Mock
    private ProcessingContextFactory processingContextFactory;

    @Mock
    private MessageProcessingContext context;


    @Mock
    private DefaultMutableConversation deadConversation;

    private final InputStream is = new ByteArrayInputStream("foo".getBytes());

    private MessageProcessingCoordinator coordinator;

    private final static byte[] SENT_MAIL = "bar".getBytes();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(guids.nextGuid()).thenReturn("1", "2", "3", "4", "5", "6", "7");
        when(mails.readMail(any(byte[].class))).thenReturn(mail);
        when(mail.getDeliveredTo()).thenReturn("foo@bar.com");
        when(mails.writeToBuffer(any(Mail.class))).thenReturn(SENT_MAIL);
        when(conversation.getImmutableConversation()).thenReturn(conversation);
        when(processingContextFactory.newContext(any(Mail.class), anyString())).thenReturn(context);
        when(context.getMessageId()).thenReturn("1");
        when(processingContextFactory.deadConversationForMessageIdConversationId(anyString(), anyString(), any(Optional.class))).thenReturn(deadConversation);
        when(context.getConversation()).thenReturn(conversation);

        coordinator = new MessageProcessingCoordinator(guids, persister, mails, flow, singletonList(messageProcessedListener),
                processingContextFactory);
    }

    @Test
    public void persistsUnparseableMail() throws Exception {
        ParsingException exception = new ParsingException("parse error");
        doThrow(exception).when(mails).readMail(any(byte[].class));

        coordinator.accept(is);

        verify(persister).persistAndIndex(deadConversation, "1", "foo".getBytes(), Optional.absent(), Termination.unparseable(exception));
    }

    @Test
    public void doesNotOfferUnparseableMailToProcessingFlow() throws Exception {
        doThrow(new ParsingException("parse error")).when(mails).readMail(any(byte[].class));

        coordinator.accept(is);

        verifyZeroInteractions(flow);
    }

    @Test
    public void passesParsableMailsToProcessingFlowPreprocessor() throws Exception {
        coordinator.accept(is);
        verify(flow).inputForPreProcessor(any(MessageProcessingContext.class));
    }

    @Test
    public void persistsUnassignableMailAsOrphaned() throws Exception {
        terminateWith(MessageState.ORPHANED, this, "orphaned", false);
        coordinator.accept(is);
        verify(persister).persistAndIndex(deadConversation, "1", "foo".getBytes(), Optional.absent(), new Termination(MessageState.ORPHANED, MessageProcessingCoordinatorTest.class, "orphaned"));
    }

    @Test
    public void persistsTerminatedMail() throws Exception {
        terminateWith(MessageState.BLOCKED, this, "blocked", true);
        coordinator.accept(is);
        verify(persister).persistAndIndex(conversation, "1", "foo".getBytes(), Optional.absent(), new Termination(MessageState.BLOCKED, MessageProcessingCoordinatorTest.class, "blocked"));
    }

    @Test
    public void persistsSuccessfulMail() throws Exception {
        when(context.hasConversation()).thenReturn(true);
        when(context.getConversation()).thenReturn(conversation);
        when(context.mutableConversation()).thenReturn(conversation);

        coordinator.accept(is);
        verify(persister).persistAndIndex(conversation, "1", "foo".getBytes(), Optional.of(SENT_MAIL), Termination.sent());
    }

    @Test(expected = Exception.class)
    public void doesNotSwallowExceptions() throws Exception {
        doThrow(new IllegalStateException()).when(flow).inputForPreProcessor(any(MessageProcessingContext.class));
        coordinator.accept(is);
    }

    @Test
    public void invokeListenerAfterPersisting() throws Exception {
        coordinator.accept(is);
        verify(messageProcessedListener)
                .messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));
    }

    @Test
    public void invokeListenerOnUnparseableMessage() throws ParsingException, IOException {
        when(mails.readMail(any(byte[].class))).thenThrow(new ParsingException());

        coordinator.accept(is);

        verify(messageProcessedListener)
                .messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));
    }

    @Test
    public void skipCrashingListener() throws Exception {
        coordinator = new MessageProcessingCoordinator(guids, persister, mails, flow,
                asList(messageProcessedListener, messageProcessedListener),
                processingContextFactory);

        doThrow(new NoClassDefFoundError())
                .doNothing()
                .when(messageProcessedListener).messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));

        coordinator.accept(is);

        verify(messageProcessedListener, times(2))
                .messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));
    }

    private void terminateWith(final MessageState state, final Object issuer, final String reason,
                               final boolean setConversation) {
        when(context.isTerminated()).thenReturn(true);
        when(context.getTermination()).thenReturn(new Termination(state, issuer.getClass(), reason));
        if (setConversation) {
            when(context.hasConversation()).thenReturn(true);
            when(context.getConversation()).thenReturn(conversation);
            when(context.mutableConversation()).thenReturn(conversation);
        }
    }

}
