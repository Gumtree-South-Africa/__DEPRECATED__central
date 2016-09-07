package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.fs.ResponsivenessFilesystemSink;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.SendResponsivenessToServiceCommand;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.ServiceDirectoryCreator;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserResponsivenessListenerTest {
    private static final long USER_ID = 1;
    private static final String CONVERSATION_ID = "1";
    private static final String MESSAGE_ID = "1";
    private UserResponsivenessListener reporter;

    @Mock
    private ResponsivenessCalculator calculator;
    @Mock
    private SendResponsivenessToServiceCommand sendToServiceCommand;
    @Mock
    private UserResponsivenessListener.CommandCreator commandCreator;
    @Mock
    private Conversation conversation;
    @Mock
    private Message message;
    @Mock
    private ca.kijiji.discovery.ServiceDirectory serviceDirectory;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private ServiceDirectoryCreator directoryWrapper;
    @Mock
    private ResponsivenessFilesystemSink filesystemSink;

    @Before
    public void setUp() throws Exception {
        doReturn(serviceDirectory).when(directoryWrapper).newServiceDirectory();

        reporter = new UserResponsivenessListener(true, commandCreator, calculator, directoryWrapper, httpClient, filesystemSink);

        doReturn(sendToServiceCommand)
                .when(commandCreator)
                .makeSendToServiceCommand(eq(serviceDirectory), eq(httpClient), anyString(), anyLong(), anyInt());
    }

    @Test
    public void reporterDisabled_noInteractions() throws Exception {
        reporter = new UserResponsivenessListener(false, commandCreator, calculator, directoryWrapper, httpClient, filesystemSink);

        reporter.messageProcessed(conversation, message);

        verifyZeroInteractions(
                httpClient, serviceDirectory, conversation, message,
                commandCreator, sendToServiceCommand, calculator, filesystemSink
        );
    }

    @Test
    public void noRecord_notReported() throws Exception {
        doReturn(null).when(calculator).calculateResponsiveness(conversation, message);

        reporter.messageProcessed(conversation, message);

        verifyZeroInteractions(commandCreator, filesystemSink);
    }

    @Test
    public void recordExists_serviceCommandExecuted() throws Exception {
        ResponsivenessRecord record = new ResponsivenessRecord(
                1, USER_ID, CONVERSATION_ID, MESSAGE_ID, 60, Instant.now()
        );

        doReturn(record).when(calculator).calculateResponsiveness(conversation, message);
        doReturn("convId").when(conversation).getId();
        doReturn("msgId").when(message).getId();

        reporter.messageProcessed(conversation, message);

        verify(commandCreator).makeSendToServiceCommand(serviceDirectory, httpClient, "convId/msgId", USER_ID, 60);
        verify(sendToServiceCommand).execute();
    }

    @Test
    public void recordExists_writtenToFilesystem() throws Exception {
        ResponsivenessRecord record = new ResponsivenessRecord(
                1, USER_ID, CONVERSATION_ID, MESSAGE_ID, 60, Instant.now()
        );

        doReturn(record).when(calculator).calculateResponsiveness(conversation, message);

        reporter.messageProcessed(conversation, message);

        verify(filesystemSink).storeResponsivenessRecord(Thread.currentThread().getName(), record);
    }

    @Test
    public void flushSinkOnShutdown() throws Exception {
        reporter.flushFilesystemSink();

        verify(filesystemSink).flushAll();
    }

    @Test
    public void serviceCommandException_suppressed() throws Exception {
        ResponsivenessRecord record = new ResponsivenessRecord(
                1, USER_ID, CONVERSATION_ID, MESSAGE_ID, 60, Instant.now()
        );

        doReturn(record).when(calculator).calculateResponsiveness(conversation, message);
        doThrow(new HystrixRuntimeException(
                HystrixRuntimeException.FailureType.COMMAND_EXCEPTION,
                SendResponsivenessToServiceCommand.class,
                "",
                new RuntimeException(),
                new RuntimeException()
        )).when(sendToServiceCommand).execute();

        reporter.messageProcessed(conversation, message);
    }
}
