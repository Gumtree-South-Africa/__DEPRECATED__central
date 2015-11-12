package nl.marktplaats.postprocessor.sendername;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.internet.InternetAddress;

import static org.mockito.Mockito.*;

public class SendernamePostProcessorTest {

    @Mock
    private Message messageMock;
    @Mock
    private Conversation conversationMock;
    @Mock
    private MutableMail mailMock;
    @Mock
    private MessageProcessingContext messageProcessingContextMock;

    private SendernamePostProcessorConfig sendernamePostProcessorConfig;
    private SendernamePostProcessor sendernamePostProcessor;
    private String[] domains = new String[0];

    private String mailOriginalFrom = "test@email.com";
    private String name = "someName";
    private String conversationId = "someConversationId";


    private String dummyBuyerNamePrefix = "dummyBuyerNamePrefix";
    private String dummySellerNamePrefix = "dummySellerNamePrefix";
    private String dummyBuyerNamePattern = dummyBuyerNamePrefix + "%s";
    private String dummySellerNamePattern = dummySellerNamePrefix + "%s";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(messageProcessingContextMock.getConversation()).thenReturn(conversationMock);
        when(conversationMock.getId()).thenReturn(conversationId);
        when(messageProcessingContextMock.getMessage()).thenReturn(messageMock);
        when(messageProcessingContextMock.getOutgoingMail()).thenReturn(mailMock);
        when(mailMock.getFrom()).thenReturn(mailOriginalFrom);
    }

    @Test
    public void testPostProcessGivenMessageBuyerToSellerAndPatternAndHeaderWhenThenFormatsNameAccordingToBuyerPatternAndSetIntoOutboundEmailFromHeader() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(mailMock.getUniqueHeader("from")).thenReturn(name);


        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verify(mailMock, times(1)).removeHeader(SendernamePostProcessor.FROM);
        verify(mailMock, times(1)).addHeader(SendernamePostProcessor.FROM, new InternetAddress(mailOriginalFrom, dummyBuyerNamePrefix + name).toString());
    }

    @Test
    public void testPostProcessGivenMessageBuyerToSellerAndPatternWhenNoConversationThenNoChangesOnEmail() {
        setUpCompleteSendernamePostProcessorConfig();

        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(messageProcessingContextMock.getConversation()).thenReturn(null);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyNoMoreInteractions(mailMock);
    }

    @Test
    public void testPostProcessGivenMessageBuyerToSellerWhenNoPatternDefinedThenNoChangesOnEmail() {
        setUpPatternlessSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyNoMoreInteractions(mailMock);
    }

    @Test(expected = IllegalStateException.class)
    public void testPostProcessGivenMessageDirectionUnknownThenExceptionIsThrown() {
        setUpCompleteSendernamePostProcessorConfig();

        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.UNKNOWN);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);
    }

    @Test
    public void testPostProcessGivenMessageSellerToBuyerAndPatternAndHeaderWhenThenFormatsNameAccordingToSellerPatternAndSetIntoOutboundEmailFromHeader() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(mailMock.getUniqueHeader("to")).thenReturn(name);


        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verify(mailMock, times(1)).removeHeader(SendernamePostProcessor.FROM);
        verify(mailMock, times(1)).addHeader(SendernamePostProcessor.FROM, new InternetAddress(mailOriginalFrom, dummySellerNamePrefix + name).toString());
    }

    @Test
    public void testPostProcessGivenMessageSellerToBuyerAndPatternWhenNoConversationThenNoChangesOnEmail() {
        setUpCompleteSendernamePostProcessorConfig();

        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(messageProcessingContextMock.getConversation()).thenReturn(null);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyNoMoreInteractions(mailMock);
    }

    @Test
    public void testPostProcessGivenMessageSellerToBuyerWhenNoPatternDefinedThenNoChangesOnEmail() {
        setUpPatternlessSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyNoMoreInteractions(mailMock);
    }

    private void setUpCompleteSendernamePostProcessorConfig() {
        sendernamePostProcessorConfig = new SendernamePostProcessorConfig(dummyBuyerNamePattern, dummySellerNamePattern);
        sendernamePostProcessor = new SendernamePostProcessor(domains, sendernamePostProcessorConfig);
    }

    private void setUpPatternlessSendernamePostProcessorConfig() {
        sendernamePostProcessorConfig = new SendernamePostProcessorConfig(null, null);
        sendernamePostProcessor = new SendernamePostProcessor(domains, sendernamePostProcessorConfig);
    }

}