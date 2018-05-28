package com.ecg.comaas.gtuk.filter.url;

import com.ecg.gumtree.MockFactory;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.UrlFilterConfig;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeUrlFilterTest.TestContext.class)
public class GumtreeUrlFilterTest {
    private static final String DISALLOWED_URL_DETECTED = "Disallowed url detected";
    private static Filter filter = mock(Filter.class);

    @Autowired
    private GumtreeUrlFilter urlFilter;

    private static ImmutableList<String> safeUrls = ImmutableList.of("gumtree.com", "gumtree.co.uk", "linkedin.com", "myspace.com",
            "facebook.com", "twitter.com", "bebo.com", "youtube.com");
    private static List<Long> exemptedCategories = ImmutableList.of(1234L, 4321L);

    @Test
    public void verifyExemptedCategoriesReturnsEmptyList() throws Exception {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        when(message.getReceivedAt()).thenReturn(new DateTime());
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);
        when(conversation.getImmutableConversation().getCreatedAt()).thenReturn(new DateTime());
        when(conversation.getImmutableConversation().getMessages()).thenReturn(ImmutableList.of(message));

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MockFactory.MESSAGE_ID,
                new ProcessingTimeGuard(1L));

        messageProcessingContext.setConversation(conversation);

        List<FilterFeedback> feedbacks = urlFilter.filter(messageProcessingContext);
        assertThat(feedbacks.size()).isEqualTo(0);
    }

    @Test
    public void verifyNoUrlsInEmpty() throws Exception {
        String emailBody = "";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifyNoUrlsInPlain() throws Exception {
        String emailBody = "This is some plain text";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    private void assertDescription(FilterFeedback reason) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(reason.getDescription());
        assertThat(node.get("description").textValue()).isEqualTo(DISALLOWED_URL_DETECTED);
    }

    @Test
    public void verifyUrlExists() throws Exception {
        String emailBody = "http://google.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertThat(reasons.get(0).getUiHint()).isEqualTo("http://google.com");
        assertDescription(reasons.get(0));
    }

    @Test
    public void verifyUrlExistsNoHttp() throws Exception {
        String emailBody = "www.google.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reasons.get(0));
    }

    @Test
    public void verifyUrlExistsDotAndSlash() throws Exception {
        String emailBody = "google.com/something";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertThat(reasons.get(0).getUiHint()).isEqualTo("google.com/");
        assertDescription(reasons.get(0));
    }

    @Test
    public void verifyNoUrlWhenJustDot() throws Exception {

        String emailBody = "Hi.is";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifyNoUrlWhenJustSingleCharTld() throws Exception {
        String emailBody = "a.b.c.d";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifyNoUrlWhenOnlySlash() throws Exception {
        String emailBody = "a/b";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);

    }

    @Test
    public void verifyUrlWithinText() throws Exception {
        String emailBody = "some text www.url.com more text";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("www.url.com");
    }

    @Test
    public void verifyUrlsInBatch() throws Exception {
        try {
            File testCaseFile = FileUtils.toFile(getClass().getResource("/urls.txt"));
            Scanner testCaseFilein = new Scanner(testCaseFile);
            while (testCaseFilein.hasNext()) {
                String emailBody = testCaseFilein.nextLine();
                List<FilterFeedback> reasons = doFilter(emailBody);
                assertThat(reasons.size()).isEqualTo(1);
                FilterFeedback reason = reasons.get(0);
                assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
                assertDescription(reason);
            }
            testCaseFilein.close();
        } catch (Exception e) {
            throw new RuntimeException("urls.txt file not found", e);
        }
    }

    @Test
    public void verifyNoUrlsInBatch() throws Exception {
        try {
            File testCaseFile = FileUtils.toFile(getClass().getResource("/not-urls.txt"));
            Scanner testCaseFilein = new Scanner(testCaseFile);
            while (testCaseFilein.hasNext()) {
                String emailBody = testCaseFilein.nextLine();
                List<FilterFeedback> reasons = doFilter(emailBody);
                assertThat(reasons.size()).isEqualTo(0);
            }
            testCaseFilein.close();
        } catch (Exception e) {
            throw new RuntimeException("not-urls.txt file not found", e);
        }
    }

    @Test
    public void verifySafeUrlList() throws Exception {
        List<String> urlPrefixes = Arrays.asList("", "www.", "http://", "https://", "file://", "http://www.",
                "something.", "http://something.");
        List<String> urlSuffixes = Arrays.asList("", "/directory", "/directory?param=value", "/!/abcabc");

        for (String safeUrl : safeUrls) {
            for (String prefix : urlPrefixes) {
                for (String suffix : urlSuffixes) {
                    String emailBody = String.format("%s%s%s", prefix, safeUrl, suffix);
                    List<FilterFeedback> reasons = doFilter(emailBody);
                    assertThat(reasons.size()).isEqualTo(0);
                }
            }

        }

    }

    @Test
    public void verifyBeboShorterLength() throws Exception {
        String emailBody = "https://bebo.com/directory";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlGumtree() throws Exception {
        String emailBody = "www.gumtree.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlGumtreeUC() throws Exception {
        String emailBody = "www.Gumtree.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlMySpace() throws Exception {

        String emailBody = "http://www.myspace.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlFacebook() throws Exception {

        String emailBody = "facebook.com/something";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlFacebookInHtmlEmail() throws Exception {

        String emailBody = "<br>facebook.com <p><td>";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlLinkedIn() throws Exception {

        String emailBody = "linkedin.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlBebo() throws Exception {

        String emailBody = "server.bebo.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifySafeUrlTwitter() throws Exception {

        String emailBody = "http://twitter.com/!/somebody.reohf";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void verifyUrlWhenAlsoSafeUrl() throws Exception {
        String emailBody = "some text www.gumtree.com but also bad url www.dodgy.com hmm";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("www.dodgy.com");
    }

    @Test
    public void verifySafeAndUnsafeUrls() throws Exception {
        String emailBody = "safe www.gumtree.com bad www.dodgy.com/verybad safe linkedin.com/dontknow bad dodgy2.com/ohdear";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("www.dodgy.com/");
    }

    @Test
    public void verifyWhenMultipleParts() throws Exception {
        String emailBody1 = "safe www.gumtree.com all good";
        String emailBody2 = "bad www.dodgy.com/verybad bad dodgy2.com/ohdear";
        List<FilterFeedback> reasons = doFilter(emailBody1, emailBody2);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("www.dodgy.com/");
    }

    @Test
    public void verifyUrlInQuotes() throws Exception {
        String emailBody = "\"www.dodgy.com\"";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
    }

    @Test
    public void verifyUrlInMultiLineEmail() throws Exception {
        String emailBody = "Hello\nThis is an email\nover multiple lines\nwith a URL http://www.dodgy.co.uk";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
    }

    @Test
    public void verifyUrlInHtml() throws Exception {
        String emailBody = "<html><body><p>Visit my website! <a href=\"www.dodgy.com\">This is a dodgy website</a></body></html>";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("\"www.dodgy.com\"");
    }

    @Test
    public void verifyUrlInHtmlEmail() throws Exception {
        String emailBody = "<br>How about checking out this site: http://www.blackbox.uk - you'll love it, I'm sure</p></td>";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("http://www.blackbox.uk");
    }

    @Test
    public void verifyUrlInMultiLineHtmlEmail() throws Exception {
        String emailBody = "<br>Multiline url\n<br>www.alibaba.cn:8080\n<br>is on second line</p></td>";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
    }

    @Test
    public void testEmailAddress() throws Exception {
        String emailBody = "This just contains an email address email@address.com";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("email@address.com");
    }

    @Test
    public void testNotFirstReply() throws Exception {
        String emailBody = "This contains a URL http://www.dodgy.com";
        List<FilterFeedback> reasons = doFilter(false, null, emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testSpecialHtml() throws Exception {
        String emailBody = "mso-para-margin-bottom:.0001pt";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testManyDotsWithSpace() throws Exception {
        String emailBody = "this is not a url... is it?";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testManyDotsNoSpace() throws Exception {
        String emailBody = "this is not a url...is it?";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testOnlyDigitsDotsNotUrl1() throws Exception {
        String emailBody = "1.2.3.4";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testOnlyDigitsDotsNotUrl2() throws Exception {
        String emailBody = "123.456.789";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testOnlyDigitsDotsNotUrl3() throws Exception {
        String emailBody = "07123.456789";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testOnlyDigitsDotsNotUrl4() throws Exception {
        String emailBody = "1.2.3.4.5.6.7.8.9";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testInWeirdBrackets() throws Exception {
        String emailBody = "Click Here <http://gummtree.rajawali6.com/Signin.htm> to update";
        List<FilterFeedback> reasons = doFilter(emailBody);
        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reasons.get(0)).isInstanceOf(FilterFeedback.class);
        assertDescription(reason);
        assertThat(reason.getUiHint()).isEqualTo("http://gummtree.rajawali6.com/");
    }

    @Test
    public void shouldAllowReplyWithUserEmailAddressInBodyForProUsers() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER, null, true);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);
        when(conversation.getImmutableConversation().getBuyerId()).thenReturn("davidsmith@mydomain.com");
        String emailBody = "test reply from davidsmith@mydomain.com";

        List<FilterFeedback> reasons = doFilter(conversation, emailBody);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void shouldBlockReplyWithUserEmailAddressInBodyForStandardUsers() throws Exception {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);
        when(conversation.getImmutableConversation().getBuyerId()).thenReturn("davidsmith@mydomain.com");
        String emailBody = "test reply from davidsmith@mydomain.com";

        List<FilterFeedback> reasons = doFilter(conversation, emailBody);

        assertThat(reasons.size()).isEqualTo(1);
    }

    @Test
    public void shouldBlockReplyWhenBadUrlAndEmailInBodyForProUser() throws Exception {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, null, true);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);
        when(conversation.getImmutableConversation().getBuyerId()).thenReturn("davidsmith@mydomain.com");
        String emailBody = "test reply from davidsmith@mydomain.com and  and www.badurl.com";

        List<FilterFeedback> reasons = doFilter(conversation, emailBody);

        assertThat(reasons.size()).isEqualTo(1);
    }

    private List<FilterFeedback> doFilter(String... emailBodies) {
        return doFilter(true, null, emailBodies);
    }

    private List<FilterFeedback> doFilter(MutableConversation conversation, String... emailBodies) {
        return doFilter(true, conversation, emailBodies);
    }

    private List<FilterFeedback> doFilter(boolean isFirstMsg, MutableConversation rawConversation, String... emailBodies) {
        long convTime;
        long msgTime;

        if (isFirstMsg) {
            convTime = System.currentTimeMillis();
            msgTime = convTime;
        } else {
            convTime = System.currentTimeMillis();
            msgTime = convTime + 1000;      // Simulate a 1 second delay between replies
        }

        DateTime convNow = new DateTime(convTime);
        DateTime msgNow = new DateTime(msgTime);

        Mail mail = mock(Mail.class);
        Message message1 = mockMessage(MessageDirection.BUYER_TO_SELLER, null, null, MockFactory.MESSAGE_ID);
        Message message2 = mockMessage(MessageDirection.BUYER_TO_SELLER, null, null, "message2");
        MutableConversation conversation = rawConversation != null ?
                rawConversation :
                mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message1);
        when(conversation.getImmutableConversation().getCreatedAt()).thenReturn(convNow);
        when(conversation.getMessageById(MockFactory.MESSAGE_ID).getReceivedAt()).thenReturn(msgNow);
        if (isFirstMsg) {
            when(conversation.getImmutableConversation().getMessages()).thenReturn(ImmutableList.of(message1, message2));
        } else {
            when(conversation.getImmutableConversation().getMessages()).thenReturn(ImmutableList.of(message2, message1));
        }

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MockFactory.MESSAGE_ID,
                new ProcessingTimeGuard(1L));

        messageProcessingContext.setConversation(conversation);

        @SuppressWarnings(value = "unchecked")
        List<TypedContent<String>> contents = new ArrayList<>();

        for (String emailBody : emailBodies) {
            contents.add(new TypedContent<String>(MediaType.ANY_TYPE, emailBody) {
                @Override
                public boolean isMutable() {
                    return false;
                }

                @Override
                public void overrideContent(String s) throws IllegalStateException {
                }
            });
        }

        when(mail.getTextParts(false)).thenReturn(contents);

        List<FilterFeedback> reasons = urlFilter.filter(messageProcessingContext);
        if (isFirstMsg) {
            assertThat(messageProcessingContext.getFilterContext().containsKey(urlFilter.getClass().getName() + ":STRIPPED-MAILS")).isTrue();
        }

        return reasons;
    }

    @Configuration
    static class TestContext {
        @Bean
        public UrlFilterConfig filterConfig() throws Exception {
            return new UrlFilterConfig.Builder(State.ENABLED, 1, Result.HOLD)
                    .withSafeUrls(safeUrls)
                    .withExemptedCategories(exemptedCategories)
                    .build();
        }

        @Bean
        public GumtreeUrlFilter filter(UrlFilterConfig filterConfig) {
            return new GumtreeUrlFilter()
                    .withPluginConfig(filter)
                    .withFilterConfig(filterConfig);
        }
    }
}
