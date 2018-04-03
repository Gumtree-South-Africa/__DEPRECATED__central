package nl.marktplaats.postprocessor.urlgateway;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.net.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.StandardEnvironment;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UrlGatewayPostProcessorTest {

    private static final String PLAIN_TEXT_CONTENT =
            "Message with a link to http://www.google.nl";
    private static final String PROCESSED_PLAIN_TEXT_CONTENT =
            "Message with a link to http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl";

    private static final String HTML_CONTENT =
            "Html message with a link to <a href=\"http://www.google.nl\">google.nl</a>.";
    private static final String PROCESSED_HTML_CONTENT =
            "Html message with a link to <a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">google.nl</a>.";

    private UrlGatewayPostProcessor processor;

    @Mock
    private Message messageMock;
    @Mock
    private Conversation conversationMock;
    @Mock
    private MutableMail mailMock;
    @Mock
    private MessageProcessingContext messageProcessingContextMock;

    @Mock
    private TypedContent<String> mutablePlainContent;
    @Mock
    private TypedContent<String> mutableHtmlContent;

    @Before
    public void setup() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://gateway.marktplaats.nl/?url=", new StandardEnvironment());

        initMocks(this);

        processor = new UrlGatewayPostProcessor(config);

        when(messageProcessingContextMock.getConversation()).thenReturn(conversationMock);
        when(messageProcessingContextMock.getMessage()).thenReturn(messageMock);
        when(messageProcessingContextMock.getOutgoingMail()).thenReturn(mailMock);

        when(mailMock.getTextParts(false)).thenReturn(asTypedContentList(mutablePlainContent, mutableHtmlContent));
        when(mutablePlainContent.getMediaType()).thenReturn(MediaType.PLAIN_TEXT_UTF_8);
        when(mutablePlainContent.isMutable()).thenReturn(true);
        when(mutablePlainContent.getContent()).thenReturn(PLAIN_TEXT_CONTENT);
        when(mutableHtmlContent.getMediaType()).thenReturn(MediaType.HTML_UTF_8);
        when(mutableHtmlContent.isMutable()).thenReturn(true);
        when(mutableHtmlContent.getContent()).thenReturn(HTML_CONTENT);
    }

    @Test
    public void testPostProcessGivenPlainAndHtmlContentTypesWhenProcessedThenEachContentIsOverridenByItsContentType() {
        processor.postProcess(messageProcessingContextMock);

        verify(mutablePlainContent).overrideContent(PROCESSED_PLAIN_TEXT_CONTENT);
        verify(mutableHtmlContent).overrideContent(PROCESSED_HTML_CONTENT);
    }

    // Circumvents problems with generics in arrays
    @SuppressWarnings({"unchecked"})
    private List<TypedContent<String>> asTypedContentList(TypedContent<String> c1, TypedContent<String> c2) {
        return Arrays.asList(c1, c2);
    }

    @Test
    public void overrideContent() {
        assertThat(processor.overrideContent(PLAIN_TEXT_CONTENT)).isEqualTo(PROCESSED_PLAIN_TEXT_CONTENT);
    }
}