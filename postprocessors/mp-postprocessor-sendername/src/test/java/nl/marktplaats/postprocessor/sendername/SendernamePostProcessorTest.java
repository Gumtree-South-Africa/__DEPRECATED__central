package nl.marktplaats.postprocessor.sendername;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.internet.InternetAddress;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

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

    private String mailOriginalFrom = "test@email.com";
    private String name = "someName";
    private String conversationId = "someConversationId";

    private String simpleBuyerNamePrefix = "simpleBuyerNamePrefix";
    private String simpleSellerNamePrefix = "simpleSellerNamePrefix";
    private String simpleBuyerNamePattern = simpleBuyerNamePrefix + "%s";
    private String simpleSellerNamePattern = simpleSellerNamePrefix + "%s";

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
    public void testPostProcessGivenMessageBuyerToSellerAndPatternAndCustomValueThenFormatsNameAccordingToBuyerPatternAndSetIntoOutboundEmailFromHeader() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(conversationMock.getCustomValues()).thenReturn(Collections.singletonMap("from", name));

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyFromReplacedWith(new InternetAddress(mailOriginalFrom, simpleBuyerNamePrefix + name).toString());
    }

    @Test
    public void testPostProcessGivenMessageBuyerToSellerAndPatternAndNoCustomValueThenNoChangesOnEmail() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(conversationMock.getCustomValues()).thenReturn(Collections.emptyMap());

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyFromReplacedWith(new InternetAddress(mailOriginalFrom, simpleBuyerNamePrefix).toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testPostProcessGivenMessageDirectionUnknownThenExceptionIsThrown() {
        setUpCompleteSendernamePostProcessorConfig();

        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.UNKNOWN);

        sendernamePostProcessor.postProcess(messageProcessingContextMock);
    }

    @Test
    public void testPostProcessGivenMessageSellerToBuyerAndPatternAndCustomValueThenFormatsNameAccordingToSellerPatternAndSetIntoOutboundEmailFromHeader() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(conversationMock.getCustomValues()).thenReturn(Collections.singletonMap("to", name));

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyFromReplacedWith(new InternetAddress(mailOriginalFrom, simpleSellerNamePrefix + name).toString());
    }

    @Test
    public void testPostProcessGivenMessageSellerToBuyerAndPatternAndNoCustomValueThenNoChangesOnEmail() throws Exception {
        setUpCompleteSendernamePostProcessorConfig();
        when(messageMock.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(conversationMock.getCustomValues()).thenReturn(Collections.emptyMap());

        sendernamePostProcessor.postProcess(messageProcessingContextMock);

        verifyFromReplacedWith(new InternetAddress(mailOriginalFrom, simpleSellerNamePrefix).toString());
    }

    private void verifyFromReplacedWith(String value) throws UnsupportedEncodingException {
        verify(mailMock, times(1)).removeHeader(Mail.FROM);
        verify(mailMock, times(1)).addHeader(Mail.FROM, value);
    }

    private void setUpCompleteSendernamePostProcessorConfig() {
        sendernamePostProcessorConfig = new SendernamePostProcessorConfig(simpleBuyerNamePattern, simpleSellerNamePattern);
        sendernamePostProcessor = new SendernamePostProcessor(sendernamePostProcessorConfig);
    }

}