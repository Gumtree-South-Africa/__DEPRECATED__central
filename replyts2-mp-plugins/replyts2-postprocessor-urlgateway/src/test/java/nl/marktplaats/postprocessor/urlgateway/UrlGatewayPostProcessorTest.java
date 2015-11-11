package nl.marktplaats.postprocessor.urlgateway;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UrlGatewayPostProcessorTest {

    private static final String DUMMY_FROM_ADDRESS = "b.anon@mail.gumtree.com";

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
    private Message message;
//    @Mock
//    private Platform platform;
//    @Mock
//    private Mail mail;
//    @Mock
//    private TypedContent<String> mutablePlainContent;
//    @Mock
//    private TypedContent<String> mutableHtmlContent;
//
//    // Circumvents problems with generics in arrays
//    @SuppressWarnings({"unchecked"})
//    private List<TypedContent<String>> asTypedContentList(TypedContent<String> c1, TypedContent<String> c2) {
//        return Arrays.asList(c1, c2);
//    }
//
//    @Before
//    public void setup() throws IOException, PersistenceException {
//        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig();
//        config.setGatewayUrl("http://gateway.marktplaats.nl/?url=");
//
//        initMocks(this);
//
//        processor = new UrlGatewayPostProcessor();
//        processor.setConfig("-1", config);
//        processor.processConfig();
//
//        when(mail.getFrom()).thenReturn(DUMMY_FROM_ADDRESS);
//        when(mail.getTextParts(false)).thenReturn(asTypedContentList(mutablePlainContent, mutableHtmlContent));
//        when(mutablePlainContent.getMediaType()).thenReturn(new MediaType("text", "plain"));
//        when(mutablePlainContent.isMutable()).thenReturn(true);
//        when(mutablePlainContent.getContent()).thenReturn(PLAIN_TEXT_CONTENT);
//        when(mutableHtmlContent.getMediaType()).thenReturn(new MediaType("text", "html"));
//        when(mutableHtmlContent.isMutable()).thenReturn(true);
//        when(mutableHtmlContent.getContent()).thenReturn(HTML_CONTENT);
//    }
//
//    @Test
//    public void testReplacements() {
//        processor.postProcess(message, platform, mail);
//
//        verify(mutablePlainContent).overrideContent(PROCESSED_PLAIN_TEXT_CONTENT);
//        verify(mutableHtmlContent).overrideContent(PROCESSED_HTML_CONTENT);
//    }


}