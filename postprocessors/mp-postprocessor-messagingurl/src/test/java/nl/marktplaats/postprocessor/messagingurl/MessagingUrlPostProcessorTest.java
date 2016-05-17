package nl.marktplaats.postprocessor.messagingurl;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MessagingUrlPostProcessorTest{

    private static final String conversationId = "m1234";
    private static final String TEXT_CONTENT =
            "Message with a link to https://www.marktplaats.nl/mijnberichten/conversationIdPlaceholder";
    private static final String  PROCESSED_TEXT_CONTENT =
            "Message with a link to https://www.marktplaats.nl/mijnberichten/" + conversationId;


    private MessagingUrlPostProcessor processor;

    @Mock
    private Conversation conversationMock;
    @Mock
    private MessageProcessingContext messageProcessingContextMock;
    @Mock
    private MutableMail mailMock;
    @Mock
    private TypedContent<String> mutableContentMock;
    @Mock
    private Message messageMock;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        processor = new MessagingUrlPostProcessor();

        when(messageProcessingContextMock.getConversation()).thenReturn(conversationMock);
        when(messageProcessingContextMock.getMessage()).thenReturn(messageMock);
        when(messageProcessingContextMock.getOutgoingMail()).thenReturn(mailMock);
        when(conversationMock.getId()).thenReturn(conversationId);
        when(mailMock.getTextParts(false)).thenReturn(Arrays.asList(mutableContentMock));
        when(mutableContentMock.isMutable()).thenReturn(true);
        when(mutableContentMock.getContent()).thenReturn(TEXT_CONTENT);

    }

    @Test
    public void testPostProcessGivenPlainAndHtmlContentTypesWhenProcessedThenEachContentIsOverridenByItsContentType() {
        processor.postProcess(messageProcessingContextMock);

        verify(mutableContentMock).overrideContent(PROCESSED_TEXT_CONTENT);
    }



}
