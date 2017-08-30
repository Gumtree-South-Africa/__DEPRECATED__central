package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@ContextConfiguration(classes = MessageProcessingCoordinatorTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@PrepareForTest(Mails.class)
@PowerMockIgnore("javax.management.*")
public class MessageProcessingCoordinatorTest {
    @MockBean
    private ProcessingFinalizer persister;

    @MockBean
    private Guids guids;

    @MockBean
    private Mail mail;

    @MockBean
    private ProcessingFlow flow;

    @MockBean
    private ProcessingContextFactory processingContextFactory;

    @MockBean
    private MessageProcessingContext context;

    @Autowired
    private DefaultMutableConversation conversation;

    @Autowired
    private MessageProcessedListener individualMessageProcessedListener;

    @Autowired
    private List<MessageProcessedListener> messageProcessedListeners;

    @Autowired
    private DefaultMutableConversation deadConversation;

    @Autowired
    private MessageProcessingCoordinator coordinator;

    private final InputStream is = new ByteArrayInputStream("foo".getBytes());

    private final static byte[] SENT_MAIL = "bar".getBytes();

    @Before
    public void setUp() throws Exception {
        mockStatic(Mails.class);

        when(guids.nextGuid()).thenReturn("1", "2", "3", "4", "5", "6", "7");
        when(Mails.readMail(any(byte[].class))).thenReturn(mail);
        when(mail.getDeliveredTo()).thenReturn("foo@bar.com");
        when(Mails.writeToBuffer(any(Mail.class))).thenReturn(SENT_MAIL);
        when(conversation.getImmutableConversation()).thenReturn(conversation);
        when(processingContextFactory.newContext(any(Mail.class), anyString())).thenReturn(context);
        when(context.getMessageId()).thenReturn("1");
        when(processingContextFactory.deadConversationForMessageIdConversationId(anyString(), anyString(), any(Optional.class))).thenReturn(deadConversation);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getOriginalTo()).thenReturn(new MailAddress("originalTo"));
        when(context.getOriginalFrom()).thenReturn(new MailAddress("originalFrom"));
    }

    @Test
    public void persistsUnparseableMail() throws Exception {
        ParsingException exception = new ParsingException("parse error");
        when(Mails.readMail(any(byte[].class))).thenThrow(exception);

        coordinator.accept(is);

        verify(persister).persistAndIndex(deadConversation, "1", "foo".getBytes(), Optional.absent(), Termination.unparseable(exception));
    }

    @Test
    public void doesNotOfferUnparseableMailToProcessingFlow() throws Exception {
        when(Mails.readMail(any(byte[].class))).thenThrow(new ParsingException("parse error"));

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
        verify(individualMessageProcessedListener)
                .messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));
    }

    @Test
    public void invokeListenerOnUnparseableMessage() throws ParsingException, IOException {
        when(Mails.readMail(any(byte[].class))).thenThrow(new ParsingException());

        coordinator.accept(is);

        verify(individualMessageProcessedListener)
                .messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));
    }

    @Test
    public void skipCrashingListener() throws Exception {
        doThrow(new NoClassDefFoundError()).when(individualMessageProcessedListener).messageProcessed(any(ImmutableConversation.class), any(ImmutableMessage.class));

        coordinator.accept(is);

        verify(individualMessageProcessedListener, times(1))
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

    @Configuration
    @Import(DefaultMessageProcessingCoordinator.class)
    static class TestContext {
        @MockBean
        private MessageProcessedListener individualMessageProcessedListener;

        @Bean
        public List<MessageProcessedListener> messageProcessedListeners() {
            return Arrays.asList(individualMessageProcessedListener);
        }

        @Bean
        public DefaultMutableConversation conversation() {
            return mock(DefaultMutableConversation.class);
        }

        @Bean
        public DefaultMutableConversation deadConversation() {
            return mock(DefaultMutableConversation.class);
        }
    }
}
