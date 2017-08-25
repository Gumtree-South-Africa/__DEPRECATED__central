package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.SendResponsivenessToServiceCommand;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink.ResponsivenessSink;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserResponsivenessListenerTest {

    private UserResponsivenessListener objectUnderTest;

    @Mock
    private ResponsivenessCalculator calculatorMock;

    @Mock
    private ResponsivenessSink sinkMock;

    @Mock
    private SendResponsivenessToServiceCommand sendCommandMock;

    @Mock
    private Conversation conversationMock;

    @Mock
    private Message messageMock;

    @Before
    public void setUp() {
        objectUnderTest = new UserResponsivenessListener(calculatorMock, sinkMock, sendCommandMock);
    }

    @Test
    public void whenNoRecord_shouldNotSendAnyData() {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(null);

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sendCommandMock, never()).execute();
        verify(sinkMock, never()).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void whenSendCommandFailed_shouldStillSendToSink() {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(getDefaultRecord());
        when(sendCommandMock.execute()).thenThrow(Exception.class);

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sendCommandMock).execute();
        verify(sinkMock).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    @Test
    public void whenRecordExists_shouldSendToHttpAndSink() {
        when(calculatorMock.calculateResponsiveness(conversationMock, messageMock)).thenReturn(getDefaultRecord());

        objectUnderTest.messageProcessed(conversationMock, messageMock);

        verify(sendCommandMock).execute();
        verify(sinkMock).storeRecord(anyString(), any(ResponsivenessRecord.class));
    }

    private static ResponsivenessRecord getDefaultRecord() {
        return new ResponsivenessRecord(1, 1L, "c1", "m1", 10, Instant.now());
    }
}
