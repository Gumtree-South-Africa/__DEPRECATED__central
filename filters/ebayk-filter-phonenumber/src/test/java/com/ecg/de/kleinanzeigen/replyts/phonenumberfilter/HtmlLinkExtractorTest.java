package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HtmlLinkExtractorTest {

    @Mock
    private TypedContent<String> textPart;

    @Test
    public void extractAnchorWithTel() {

        when(textPart.getContent()).thenReturn("<a href=\"tel:+491234567\"");

        HtmlLinkExtractor extractor = new HtmlLinkExtractor();
        NumberStream numberStream = extractor.extractStream(textPart);

        assertThat(numberStream.getItems()).containsExactly("491234567");
    }

    @Test
    public void extractAnchorWithTelWithWhitespaces() {

        when(textPart.getContent()).thenReturn("<a href=\"  tel:+491234567\"");

        HtmlLinkExtractor extractor = new HtmlLinkExtractor();
        NumberStream numberStream = extractor.extractStream(textPart);

        assertThat(numberStream.getItems()).containsExactly("491234567");
    }

    @Test
    public void cleanupNumber() {

        when(textPart.getContent()).thenReturn("<a href=\"tel:+49(123) 45 67\"");

        HtmlLinkExtractor extractor = new HtmlLinkExtractor();
        NumberStream numberStream = extractor.extractStream(textPart);

        assertThat(numberStream.getItems()).containsExactly("491234567");
    }

    @Test
    public void ignoreAnchorWithoutTel() {

        when(textPart.getContent()).thenReturn("<a href=\"491234567\"");

        HtmlLinkExtractor extractor = new HtmlLinkExtractor();
        NumberStream numberStream = extractor.extractStream(textPart);

        assertThat(numberStream.getItems()).isEmpty();
    }

}
