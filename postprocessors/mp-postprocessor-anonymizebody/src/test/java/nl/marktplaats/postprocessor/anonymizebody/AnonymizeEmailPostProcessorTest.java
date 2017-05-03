package nl.marktplaats.postprocessor.anonymizebody;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AnonymizeEmailPostProcessorTest {
    private String unanonymizedSender = "unanonymized@hotmail.com";
    private String anonymizedSender = "anonymized@gumtree.com";

    private Message message = mock(Message.class);
    private Conversation conversation = mock(Conversation.class);
    private TypedContent<String> messageContent = mock(TypedContent.class);

    private AnonymizeEmailPostProcessorConfig anonymizeEmailPostProcessorConfig;
    private AnonymizeEmailPostProcessor postProcessor;

    @Before
    public void setup() throws PersistenceException, IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/anonymizeemail.properties"));
        List<String> patterns = props.values().stream().map(Object::toString).collect(Collectors.toList());

        anonymizeEmailPostProcessorConfig = new AnonymizeEmailPostProcessorConfig(patterns);
        postProcessor = new AnonymizeEmailPostProcessor(new String[]{"mail.marktplaats.nl"}, anonymizeEmailPostProcessorConfig);

        // create mock conversation and repository
        when(conversation.getUserId(Matchers.<ConversationRole>any())).thenReturn(unanonymizedSender);

        // create mock message
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
    }
    
    private MessageProcessingContext prepareContext(Conversation conversation, Message message, MutableMail mail) {
        MessageProcessingContext messageProcessingContext = mock(MessageProcessingContext.class);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(messageProcessingContext.getOutgoingMail()).thenReturn(mail);
        return messageProcessingContext;
    }

    @Test
    public void testReplaceNone() {
        String text = "This message was sent from " + anonymizedSender + ".";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were not adjusted
        verify(messageContent, never()).overrideContent(anyString());
    }

    @Test
    public void testReplaceOne() {
        String text = "This message was sent from: " + unanonymizedSender + ".";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("This message was sent from: " + anonymizedSender + "."));
    }

    @Test
    public void testReplaceAll() {
        String text = "This message was sent from: " + unanonymizedSender + " (" + unanonymizedSender + ").";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("This message was sent from: " + anonymizedSender + " (" + anonymizedSender + ")."));
    }

    @Test
    public void testReplaceBodyOnly() {
        String text = "This is my email address " + unanonymizedSender
                + " thanks. To: " + unanonymizedSender + ". From:  " + unanonymizedSender + ".";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test a
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(
                "This is my email address "+ unanonymizedSender
                        + " thanks. To: " + anonymizedSender + ". From:  " + anonymizedSender + "."));
    }

    @Test
    public void testReplaceBodyOnlyForVanCaseWithLTGT() throws IOException {
        String text = loadFileAsString("/nl/marktplaats/postprocessor/anonymizebody/emailAnswer.txt");
        String cutString = "<b>Van:</b> petra barbier &lt;dollydot94@hotmail.com&gt;<br>";
        assertTrue(text.contains(cutString));

        MutableMail mail = prepareMailWithPainText(text);

        // call method under test a
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());

        assertFalse(argument.getValue().contains(cutString));
    }

    @Test
    public void testToRegex() {
        String text = "To: " + unanonymizedSender;
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: " + anonymizedSender));
    }

    @Test
    public void testToRegexWithNewline() {
        String text = "To: \n" + unanonymizedSender;
        Charset charset = Charset.forName("ISO-8859-1");

        MutableMail mail = prepareMailWithHtml(text, charset);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);         
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: \n" + anonymizedSender));
    }

    @Test
    public void testDeliberateLeakWithAnchorTags() {
        String text = "<a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were not adjusted
        verify(messageContent, never()).overrideContent(anyString());
    }

    @Test
    public void testWithAnchorTagsWithTo() {
        String text = "To: <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + anonymizedSender + "\">" + anonymizedSender + "</a>"));
    }

    @Test
    public void testWithAnchorTagsWithBoldTo() {
        String text = "<b>To:</b> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("<b>To:</b> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + anonymizedSender + "\">" + anonymizedSender + "</a>"));
    }

    @Test
    public void testWithAnchorTagsWithSpannedTo() {
        String text = "<span>To:</span> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("<span>To:</span> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + anonymizedSender + "\">" + anonymizedSender + "</a>"));
    }

    @Test
    public void testWithAnchorTagsWithBoldAndSpannedTo() {
        String text = "<b><span>To:</span></b> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("<b><span>To:</span></b> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + anonymizedSender + "\">" + anonymizedSender + "</a>"));
    }

    @Test
    public void testWithAnchorTagsWithSpanAndBoldTo() {
        String text = "<span><b>To:</b></span> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + unanonymizedSender + "\">" + unanonymizedSender + "</a>";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test   abc
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("<span><b>To:</b></span> <a class=\"moz-txt-link-abbreviated\" " +
                "href=\"mailto:" + anonymizedSender + "\">" + anonymizedSender + "</a>"));
    }

    @Test
    public void testWithLtGtBrackets() {
        String text = "To: <" + unanonymizedSender + ">";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: <" + anonymizedSender + ">"));
    }

    @Test
    public void testWithLtGtEscBrackets() {
        String text = "To: &lt;" + unanonymizedSender + "&gt;";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: &lt;" + anonymizedSender + "&gt;"));
    }

    @Test
    public void testWithNameAndLtGtBrackets() {
        String text = "To: Andy Summers <" + unanonymizedSender + ">";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: Andy Summers <" + anonymizedSender + ">"));
    }

    @Test
    public void testWithNameAndLtGtEscBrackets() {
        String text = "To: Andy Summers &lt;" + unanonymizedSender + "&gt;";
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: Andy Summers &lt;" + anonymizedSender + "&gt;"));
    }

    @Test
    public void testDeliberateMailLeakAfterNameAndLtGtEscBrackets() {
        String text = "To: Andy Summers &lt;" + unanonymizedSender + "&gt;\n" +
                "Hi my email address is " + unanonymizedSender;
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify some contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: Andy Summers &lt;" + anonymizedSender + "&gt;\n" +
                "Hi my email address is " + unanonymizedSender));
    }

    @Test
    public void testDeliberateEmailLeakAfterToWithNameOnly() {
        String text = "To: Sunny Ajax \n" +
                "Hi my email address is <" + unanonymizedSender + ">";

        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were not adjusted
        verify(messageContent, never()).overrideContent(anyString());
    }

    @Test
    public void testWithLongNameAndBracketed() {
        String text = "To: Andy dos santos Summers <" + unanonymizedSender + ">\n" +
                "Hi my email address is <" + unanonymizedSender + ">";

        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: Andy dos santos Summers <" + anonymizedSender + ">\n" +
                "Hi my email address is <" + unanonymizedSender + ">"));
    }

    @Test
    public void testThirdPartyMailAddressesAreStripped() {
        String text ="> Aan: Another Guy <third.party@hotmail.com>\n" +
                "Hi my email address is third.party@hotmail.com.";

        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("> \n" +
                "Hi my email address is third.party@hotmail.com."));
    }

    @Test
    public void testRegressionSuite() {
        String text = "To:AgeBobCyrilAgnesMargeWalker&lt;s.31231211.BC1@mail.gumtree.com&gt;\n" +
                "To:AgeBobCyrilAgnesMargeWalker <" + unanonymizedSender + ">\n" +
                "From:Walker BobCyrilAgnesMargeAdrianne&lt;b.31231211.BC1@mail.gumtree.com&gt;\n" +
                "From:Walker BobCyrilAgnesMargeAdrianne <" + unanonymizedSender + ">";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("\n" +
                "To:AgeBobCyrilAgnesMargeWalker <" + anonymizedSender + ">\n" +
                "\n" +
                "From:Walker BobCyrilAgnesMargeAdrianne <" + anonymizedSender + ">"));
    }

    @Test
    public void testRegressionSuite2() {
        String text = "To: agewalker&lt;s.31231211.BC1@mail.gumtree.com&gt;\n" +
                "To: Walker Adrianne<" + unanonymizedSender + ">\n" +
                "From: adwalker&lt;b.31231211.BC1@mail.gumtree.com&gt;\n" +
                "From: Walker Adrianne<" + unanonymizedSender + ">";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("\n" +
                "To: Walker Adrianne<" + anonymizedSender + ">\n" +
                "\n" +
                "From: Walker Adrianne<" + anonymizedSender + ">"));
    }

    @Test
    public void testRegressionSuite3() {
        String text = "To&#58; unanonymized&#64;hotmail.com";
        Charset charset = Charset.forName("ISO-8859-1");

        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(text, charset);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("To: " + anonymizedSender));
    }

    @Test
    public void testRegressionSuite4_largeTextPart() throws Exception {
        String text = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/plain-part.txt"), "US-ASCII"));
        String expected = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/plain-part-replaced.txt"), "US-ASCII"));

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(expected));
    }

    @Test
    public void testRegressionSuite5_largeHtmlPart() throws Exception {
        String text = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/html-part.html"), "ISO-8859-1"));
        String expected = CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/html-part-replaced.html"), "US-ASCII"));

        // create mock mail and content
        Charset charset = Charset.forName("ISO-8859-1");
        MutableMail mail = prepareMailWithHtml(text, charset);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(expected));
    }

    @Test
    public void testRegressionSuite6_spaceBeforeColon() {
        String text = "Aan : " + unanonymizedSender;

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text);

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("Aan : " + anonymizedSender));
    }

    @Test
    public void testWithRegularExpressionSyntax() {

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText("From: some name [mailto:" + unanonymizedSender + "]");

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo("From: some name [mailto:" + anonymizedSender + "]"));
    }

    @Test
    public void testRegressionSuiteMailFromMicrosoftOfficeOutlook11() {
        String html = "<b><span style='font-weight:bold'>Aan:</span></b> " +
                "<st1:PersonName w:st=\"on\">%s</st1:PersonName><br>\n";

        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(String.format(html, unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(String.format(html, anonymizedSender)));
    }

    @Test
    public void testRegressionSuiteMailFromMicrosoftOutlookCom() {
        String html = "<br><b>To:</b>&nbsp;<a href=\"mailto:%s\" target=\"_parent\">Mr. Mailer</a></font></div></div><div><br>";

        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(String.format(html, unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(String.format(html, anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_newLinesOnRawContent() throws Exception {
        InputStream eml = getClass().getResourceAsStream("/new-lines-in-encoded-form.eml");
        InputStream expectedEml = getClass().getResourceAsStream("/new-lines-in-encoded-form-replaced.eml");

        Mails parser = new Mails();
        MutableMail mail = parser.readMail(ByteStreams.toByteArray(eml)).makeMutableCopy();

        Mail expectedMail = parser.readMail(ByteStreams.toByteArray(expectedEml));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        String[] actualContentParts = extractContentParts(mail);
        String[] expectedContentParts = extractContentParts(expectedMail);

        assertThat("text parts should match", actualContentParts[0], equalTo(expectedContentParts[0]));
        assertThat("html parts should match", actualContentParts[1], equalTo(expectedContentParts[1]));
    }

    @Test
    public void testRegressionSuite_multilineHtml() {
        String html = "<DIV style=\"font-color: black\"><B>From:</B> <A  \r\n" +
                "title=a.12fhasfjdadg@mail.marktplaats.nl  \r\n" +
                "href=\"mailto:a.12fhasfjdadg@mail.marktplaats.nl\">Rutger via Marktplaats</A>  \r\n" +
                "</DIV> \r\n" +
                "<DIV><B>Sent:</B> Wednesday, June 25, 2014 12:01 PM</DIV> \r\n" +
                "<DIV>To: {{{sender}}}</DIV> \r\n" +
                "<DIV><B>To:</B> <A title={{{sender}}}  \r\n" +
                "href=\"mailto:{{{sender}}}\">{{{sender}}}</A> </DIV> \r\n" +
                "<DIV><B>To:</B> <A href=\"mailto:{{{sender}}}\">  \r\n" +
                "{{{sender}}}</A> </DIV> \r\n" +
                "<<DIV><B>To:</B> <A title=\"{{{sender}}}\"  \r\n" +
                "href=\"mailto:{{{sender}}}\">{{{sender}}}</A> </DIV> \r\n" +
                "<DIV><B>To:</B> <A title={{{sender}}}  \r\n" +
                "href=\"mailto:{{{sender}}}\">{{{sender}}}</A> </DIV> \r\n" +
                "<DIV><B>Subject:</B> Re: Reactie: Oranje poppetjes C1000 en  \r\n" +
                "Xenos.</DIV></DIV></DIV> \r\n" +
                "<DIV>&nbsp;</DIV></DIV> \r\n";


        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(html.replace("{{{sender}}}", unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(html.replace("{{{sender}}}", anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_aur287_multilineHtml_3() {
        String html = "<div>Great</div>\n" +
                "<div>Check now my email</div>\n" +
                "<div><br>\n" +
                "</div>\n" +
                "<span id=\"OLK_SRC_BODY_SECTION\">\n" +
                "<div style=\"font-family:Calibri; font-size:11pt; text-align:left; color:black; BORDER-BOTTOM: medium none; BORDER-LEFT: medium none; PADDING-BOTTOM: 0in; PADDING-LEFT: 0in; PADDING-RIGHT: 0in; BORDER-TOP: #b5c4df 1pt solid; BORDER-RIGHT: medium none; PADDING-TOP: 3pt\">\n" +
                "<span style=\"font-weight:bold\">From: </span>Ik via Marktplaats &lt;<a href=\"mailto:a.bucrbf2y3rkd9@mail.marktplaats.nl\">a.bucrbf2y3rkd9@mail.marktplaats.nl</a>&gt;<br>\n" +
                "<span style=\"font-weight:bold\">Date: </span>Friday 2 October 2015 16:23<br>\n" +
                "<span style=\"font-weight:bold\">To: </span>&quot;Rossi, Mario &quot; &lt;<a href=\"mailto:{{{sender}}}\">{{{sender}}}</a>&gt;<br>\n" +
                "<span style=\"font-weight:bold\">Subject: </span>Ik heb interesse in 'just test' - Ik<br>\n" +
                "</div>\n" +
                "<div><br>\n" +
                "</div>";


        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(html.replace("{{{sender}}}", unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(html.replace("{{{sender}}}", anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_mig8953_newPatternHtml() {
        String html = "Op 4 mrt. 2015 23:03 schreef &quot;Jaap Sterren&quot; &lt;" +
                "<a href=\"mailto:{{{sender}}}\">{{{sender}}}</a>&gt;:<br>\n";

        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(html.replace("{{{sender}}}", unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(html.replace("{{{sender}}}", anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_mig8953_newPatternPlainText() {
        String text = "Op 4 mrt. 2015 23:03 schreef \"Jaap Sterren\" <{{{sender}}}>:\n";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text.replace("{{{sender}}}", unanonymizedSender));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(text.replace("{{{sender}}}", anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_mig8953_multilineText() {
        String text = "Hallo juul met neel gaat het niet meer door ik hoor graag nog wat van je\n" +
                "grtjes van neel\n" +
                "Op 4 mrt. 2015 23:03 schreef \"Jaap Sterren\" <\n" +
                "{{{sender}}}>:\n" +
                ">\n" +
                "> Oke juul ik ga ze los verkopen  voor 35 mag je  ze over nemen ze zien er\n" +
                "nog heel goed uit + 1,28 voor het verzenden of wil je track en trak? Ik\n" +
                "weet niet zo goed hoe het heet  via het post kantoor of aan getekend voor\n" +
                "6,75 of kom je ze halen grtjes van neel hoor ik nog wat van je?) oja ik had\n" +
                "nog een foto gemaakt heb je het nog gezien niet zo duidelijk het merkje\n" +
                "staat er ook nog goed op  welterusten\n";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text.replace("{{{sender}}}", unanonymizedSender));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(text.replace("{{{sender}}}", anonymizedSender)));
    }

    @Test
    public void testRegressionSuite_aur287_multilineText_2() {
        String text = "From: Ik via Marktplaats <a.bucrbf2y3rkd9@mail.marktplaats.nl<mailto:a.bucrbf2y3rkd9@mail.marktplaats.nl>>\n" +
                "Date: Friday 2 October 2015 16:23\n" +
                "To: \"Rossi, Mario\" <{{{sender}}}<mailto:{{{sender}}}>>\n" +
                "Subject: Ik heb interesse in 'just test' - Ik";

        // create mock mail and content
        MutableMail mail = prepareMailWithPainText(text.replace("{{{sender}}}", unanonymizedSender));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(text.replace("{{{sender}}}", anonymizedSender)));
    }


    @Test
    public void testRegressionSuite_multilineHtml_2() {
        String html = "<div dir=\"ltr\">\n" +
                "<hr>\n" +
                "<span style=\"font-family: Calibri,sans-serif; font-size: 11pt; font-weight: bold;\">Van:\n" +
                "</span><span style=\"font-family: Calibri,sans-serif; font-size: 11pt;\"><a href=\"mailto:a.23rj0hbfsopsg@mail.marktplaats.nl\">Bram via Marktplaats</a></span><br>\n" +
                "<span style=\"font-family: Calibri,sans-serif; font-size: 11pt; font-weight: bold;\">Verzonden:\n" +
                "</span><span style=\"font-family: Calibri,sans-serif; font-size: 11pt;\">\u200E7-\u200E3-\u200E2015 08:18</span><br>\n" +
                "<span style=\"font-family: Calibri,sans-serif; font-size: 11pt; font-weight: bold;\">Aan:\n" +
                "</span><span style=\"font-family: Calibri,sans-serif; font-size: 11pt;\"><a href=\"mailto:{{{sender}}}\">{{{sender}}}</a></span><br>\n" +
                "<span style=\"font-family: Calibri,sans-serif; font-size: 11pt; font-weight: bold;\">Onderwerp:\n" +
                "</span><span style=\"font-family: Calibri,sans-serif; font-size: 11pt;\">Re: Reactie Bod: Mooie Trek Racefiets, maat 56</span><br>\n" +
                "<br>\n" +
                "</div>\n";

        // create mock mail and content
        MutableMail mail = prepareMailWithHtml(html.replace("{{{sender}}}", unanonymizedSender), Charset.forName("UTF8"));

        // call method under test
        MessageProcessingContext messageProcessingContext = prepareContext(conversation, message, mail);
        postProcessor.postProcess(messageProcessingContext);

        // verify contents of mail message were adjusted
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(messageContent).overrideContent(argument.capture());
        assertThat(argument.getValue(), equalTo(html.replace("{{{sender}}}", anonymizedSender)));
    }

    private String loadFileAsString(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }
    }

    private String[] extractContentParts(Mail mail) {
        String htmlPartContent = null;
        String textPartContent = null;
        for (TypedContent<String> textPart : mail.getTextParts(false)) {
            if (!MediaTypeHelper.isHtmlCompatibleType(textPart.getMediaType())) {
                textPartContent = textPart.getContent();
            }
            if (MediaTypeHelper.isHtmlCompatibleType(textPart.getMediaType())) {
                htmlPartContent = textPart.getContent();
            }
        }
        return new String[] {textPartContent, htmlPartContent};
    }

    private MutableMail prepareMailWithPainText(String text) {
        MutableMail mail = mock(MutableMail.class);
        when(mail.getFrom()).thenReturn(anonymizedSender);
        List<TypedContent<String>> textParts = createTextParts(text);
        when(mail.getTextParts(false)).thenReturn(textParts);
        return mail;
    }

    private MutableMail prepareMailWithHtml(String text, Charset charset) {
        MutableMail mail = mock(MutableMail.class);
        when(mail.getFrom()).thenReturn(anonymizedSender);
        List<TypedContent<String>> textParts = createTextPartsHtml(text, charset);
        when(mail.getTextParts(false)).thenReturn(textParts);
        return mail;
    }

    private List<TypedContent<String>> createTextParts(String textPart) {
        when(messageContent.isMutable()).thenReturn(true);
        when(messageContent.getContent()).thenReturn(textPart);
        when(messageContent.getMediaType()).thenReturn(MediaType.create("text", "plain"));
        List<TypedContent<String>> textParts = new ArrayList<>();
        textParts.add(messageContent);
        return textParts;
    }

    private List<TypedContent<String>> createTextPartsHtml(String textPart, Charset charset) {
        when(messageContent.isMutable()).thenReturn(true);
        when(messageContent.getContent()).thenReturn(textPart);
        when(messageContent.getMediaType()).thenReturn(MediaType.create("text", "html").withCharset(charset));
        List<TypedContent<String>> textParts = new ArrayList<>();
        textParts.add(messageContent);
        return textParts;
    }
}
