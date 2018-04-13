package com.ecg.comaas.kjca.listener.userbehaviour;

import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.HystrixCommandConfigurationProvider;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.service.SendResponsivenessToServiceCommand;
import com.ecg.comaas.kjca.listener.userbehaviour.reporter.sink.ResponsivenessSink;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.retry.RetryException;
import com.netflix.hystrix.HystrixCommand;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserResponsivenessListenerTest {

    private static final String DEFAULT_SCHEMA = "http";
    private static final String DEFAULT_HOST = "localhost";
    private static final Integer DEFAULT_PORT = 80;

    private UserResponsivenessListener objectUnderTest;

    @Mock
    private ResponsivenessCalculator calculatorMock;

    @Mock
    private ResponsivenessSink sinkMock;

    @Mock
    private SendResponsivenessToServiceCommand sendResponsivenessCommandMock;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private Conversation conversationMock;

    @Mock
    private Message messageMock;

    private HystrixCommand.Setter userBehaviourHystrixConfig = HystrixCommandConfigurationProvider.provideUserBehaviourConfig(true);

    @Before
    public void setUp() {
        objectUnderTest = Mockito.spy(new UserResponsivenessListener(calculatorMock, sinkMock, httpClient, userBehaviourHystrixConfig,
                DEFAULT_SCHEMA, DEFAULT_HOST, DEFAULT_PORT));
        when(objectUnderTest.createHystrixCommand()).thenReturn(sendResponsivenessCommandMock);
    }

    @Test
    public void whenNoRecord_shouldNotSendAnyData() throws RetryException {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(null);

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sendResponsivenessCommandMock, never()).execute();
        verify(sinkMock, never()).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void whenSendCommandFailed_shouldStillSendToSink() throws RetryException {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(getDefaultRecord());
        when(sendResponsivenessCommandMock.execute()).thenThrow(Exception.class);

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sinkMock).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    @Test
    public void whenRecordExists_shouldSendToHttpAndSink() throws RetryException {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(getDefaultRecord());

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sendResponsivenessCommandMock).execute();
        verify(sinkMock).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    private static ResponsivenessRecord getDefaultRecord() {
        return new ResponsivenessRecord(1, 1L, "c1", "m1", 10, Instant.now());
    }
}
