package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
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
    private Mail mail;

    @MockBean
    private MutableMail mutableMail;

    @MockBean
    private ProcessingFlow flow;

    @MockBean
    private ProcessingContextFactory processingContextFactory;

    @MockBean
    private MessageProcessingContext context;

    @Captor
    private ArgumentCaptor<Optional<byte[]>> receivedBytesCaptor;

    @Captor
    private ArgumentCaptor<Optional<byte[]>> sentBytesCaptor;

    @Autowired
    private DefaultMutableConversation conversation;

    @Autowired
    private MessageProcessedListener individualMessageProcessedListener;

    @Autowired
    private DefaultMutableConversation deadConversation;

    @Autowired
    private MessageProcessingCoordinator coordinator;

    private static final byte[] RECEIVED_BYTES = "foo".getBytes();

    private final InputStream is = new ByteArrayInputStream(RECEIVED_BYTES);

    private final static byte[] SENT_BYTES = "bar".getBytes();

    @Before
    public void setUp() throws Exception {
        mockStatic(Mails.class);

        when(Mails.readMail(any(byte[].class))).thenReturn(mail);
        when(mail.getDeliveredTo()).thenReturn("foo@bar.com");
        when(Mails.writeToBuffer(any(Mail.class))).thenReturn(SENT_BYTES);
        when(conversation.getImmutableConversation()).thenReturn(conversation);
        when(processingContextFactory.newContext(any(Mail.class), anyString())).thenReturn(context);
        when(context.getMessageId()).thenReturn("1");
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(processingContextFactory.deadConversationForMessageIdConversationId(anyString(), anyString(), any(Optional.class))).thenReturn(deadConversation);
        when(context.getConversation()).thenReturn(conversation);
        when(mail.getDeliveredTo()).thenReturn("originalTo");
        when(mail.getFrom()).thenReturn("originalFrom");
        when(context.getOutgoingMail()).thenReturn(mutableMail);
    }

    @Test
    public void persistsUnparseableMail() throws Exception {
        ParsingException exception = new ParsingException("parse error");
        when(Mails.readMail(any(byte[].class))).thenThrow(exception);

        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> {
            coordinator.accept(is);
        });

        verify(persister).persistAndIndex(eq(deadConversation), anyString(), receivedBytesCaptor.capture(), eq(Optional.empty()), eq(Termination.unparseable(exception)));
        assertThat(receivedBytesCaptor.getValue().get()).isEqualTo(RECEIVED_BYTES);
    }

    @Test
    public void doesNotOfferUnparseableMailToProcessingFlow() throws Exception {
        when(Mails.readMail(any(byte[].class))).thenThrow(new ParsingException("parse error"));

        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> {
            coordinator.accept(is);
        });

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
        verify(persister).persistAndIndex(eq(deadConversation), eq("1"), receivedBytesCaptor.capture(), sentBytesCaptor.capture(), eq(new Termination(MessageState.ORPHANED, MessageProcessingCoordinatorTest.class, "orphaned")));
        assertThat(receivedBytesCaptor.getValue()).contains(RECEIVED_BYTES);
        assertThat(sentBytesCaptor.getValue()).isEmpty();
    }

    @Test
    public void persistsTerminatedMail() throws Exception {
        terminateWith(MessageState.BLOCKED, this, "blocked", true);
        coordinator.accept(is);
        verify(persister).persistAndIndex(eq(conversation), eq("1"), receivedBytesCaptor.capture(), sentBytesCaptor.capture(), eq(new Termination(MessageState.BLOCKED, MessageProcessingCoordinatorTest.class, "blocked")));
        assertThat(receivedBytesCaptor.getValue()).contains(RECEIVED_BYTES);
        assertThat(sentBytesCaptor.getValue()).isEmpty();
    }

    @Test
    public void persistsSuccessfulMail() throws Exception {
        when(context.hasConversation()).thenReturn(true);
        when(context.getConversation()).thenReturn(conversation);
        when(context.mutableConversation()).thenReturn(conversation);

        coordinator.accept(is);
        verify(persister).persistAndIndex(eq(conversation), eq("1"), receivedBytesCaptor.capture(), sentBytesCaptor.capture(), eq(Termination.sent()));
        assertThat(receivedBytesCaptor.getValue()).contains(RECEIVED_BYTES);
        assertThat(sentBytesCaptor.getValue()).contains(SENT_BYTES);
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

        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> {
            coordinator.accept(is);
        });

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
