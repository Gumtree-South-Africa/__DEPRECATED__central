package com.ecg.gumtree.comaas.filter.integration;

import com.ecg.gumtree.comaas.filter.blacklist.GumtreeBlacklistFilter;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.gumtree.common.util.http.NotFoundHttpStatusException;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.BlacklistFilterConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.spec.ChecklistApi;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistAttribute;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistEntry;
import com.gumtree.gumshield.api.domain.checklist.ApiChecklistType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GumtreeBlacklistFilterTest.TestContext.class)
public class GumtreeBlacklistFilterTest {
    private static final String MESSAGE_ID = "123";
    private static final String RECIPIENT_BLACKLISTED_DESCRIPTION = "Recipient blacklisted";
    private static final String SENDER_BLACKLISTED_DESCRIPTION = "Sender blacklisted";
    private static final String DESCRIPTION = "description";

    private Set<Long> categoryBreadCrumb = ImmutableSet.of(123L, 234L);
    private static List<Long> exemptedCategories = ImmutableList.of(1234L, 4321L);

    @Mock
    private Mail mail;

    @Autowired
    private GumtreeBlacklistFilter filter;

    @MockBean
    private GumshieldApi gumshieldApi;

    @MockBean
    private ChecklistApi checklistApi;

    @Before
    public void init() {
        initialiseChecklistApiTestConditions();
    }

    @Test
    public void testExemptedCategoriesReturnsSender() {
        Set<Long> categoryBreadCrumbWithExemptedCat = ImmutableSet.of(123L, 234L, 1234L);
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", categoryBreadCrumbWithExemptedCat);

        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testKnownSender() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testUnknownSenderAndUnknownRecipient() {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("goodguy@yahoo.com", "goodguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MESSAGE_ID, testProcessingTimeGuard());

        messageProcessingContext.setConversation(conversation);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testUnknownSenderAndUnknownRecipientButKnownAlias() throws Exception {
        when(mail.getFrom()).thenReturn("badguy@hotmail.com");
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("goodguy@yahoo.com", "goodguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MESSAGE_ID, testProcessingTimeGuard());

        messageProcessingContext.setConversation(conversation);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertJsonField(reason, DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
        assertThat(reason.getUiHint()).isEqualTo("badguy@hotmail.com");
    }

    @Test
    public void testKnownAliasKnownSenderAndKnownRecipient() throws Exception {
        when(mail.getFrom()).thenReturn("badguy@hotmail.com");
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("badguy@hotmail.com", "badguy@yahoo.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MESSAGE_ID, testProcessingTimeGuard());

        messageProcessingContext.setConversation(conversation);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(2);
        assertJsonField(reasons.get(0), DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
        assertJsonField(reasons.get(1), DESCRIPTION, RECIPIENT_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testKnownRecipient() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerGoodBuyerBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, RECIPIENT_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testKnownRecipientAndKnownSender() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("badguy@yahoo.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MESSAGE_ID, testProcessingTimeGuard());

        messageProcessingContext.setConversation(conversation);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(2);
        assertJsonField(reasons.get(0), DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
        assertJsonField(reasons.get(1), DESCRIPTION, RECIPIENT_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testSellerToBuyerSenderBlacklistedSenderAccountHolder() {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER, true, null);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerGoodBuyerBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testSellerToBuyerRecipientBlacklistedRecipientAccountHolder() {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testSellerToBuyerRecipientBlacklistedSenderAccountHolder() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerGoodBuyerBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, RECIPIENT_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testSellerToBuyerSenderBlacklistedRecipientAccountHolder() throws Exception {
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER, true, null);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testBuyerToSellerRecipientBlacklistedRecipientAccountHolder() {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testBuyerToSellerRecipientBlacklistedSenderAccountHolder() throws Exception {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, true, null);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerBadBuyerGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, RECIPIENT_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testBuyerToSellerSenderBlacklistedRecipientAccountHolder() throws Exception {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerGoodBuyerBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        assertJsonField(reasons.get(0), DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
    }

    @Test
    public void testBuyerToSellerSenderBlacklistedSenderAccountHolder() {
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, true, null);
        MessageProcessingContext messageProcessingContext = getMessageContextSellerGoodBuyerBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testSenderEmailDomainIsBlacklisted() throws Exception {
        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenReturn(new ApiChecklistEntry());


        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextBuyerEmailDomainBad(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertJsonField(reason, DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
        assertThat(reason.getUiHint()).isEqualTo("goodguy@hotmail.com");
    }


    @Test
    public void testSenderIpAddressIsBlacklisted() throws Exception {
        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenThrow(new NotFoundHttpStatusException());
        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.HOST),
                eq("1.1.1.1"))).thenReturn(new ApiChecklistEntry());


        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER, null, true);
        MessageProcessingContext messageProcessingContext = getMessageContextEmailsGood(message);
        List<FilterFeedback> reasons = filter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertJsonField(reason, DESCRIPTION, SENDER_BLACKLISTED_DESCRIPTION);
        assertThat(reason.getUiHint()).isEqualTo("1.1.1.1");
    }


    private void assertJsonField(FilterFeedback reason, String field, String expected) throws Exception {
        JsonNode actualObj = new ObjectMapper().readTree(reason.getDescription());
        assertThat(actualObj.get(field).textValue()).isEqualTo(expected);
    }

    private ProcessingTimeGuard testProcessingTimeGuard() {
        return new ProcessingTimeGuard(1L);
    }

    private MessageProcessingContext getMessageContextSellerGoodBuyerBad(Message message) {
        MutableConversation conversation = mockConversation("badguy@hotmail.com", "goodguy@hotmail.com", message);
        return contextFromConversation(conversation);
    }

    private MessageProcessingContext getMessageContextSellerBadBuyerGood(Message message) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);
        return contextFromConversation(conversation);
    }

    private MessageProcessingContext getMessageContextBuyerEmailDomainBad(Message message) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "goodguy@yahoo.com", message);
        return contextFromConversation(conversation);
    }

    private MessageProcessingContext getMessageContextEmailsGood(Message message) {
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "goodguy@yahoo.com", message);
        return contextFromConversation(conversation);
    }

    private MessageProcessingContext contextFromConversation(MutableConversation conversation) {
        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, MESSAGE_ID, testProcessingTimeGuard());
        messageProcessingContext.setConversation(conversation);
        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", categoryBreadCrumb);
        return messageProcessingContext;
    }

    private void initialiseChecklistApiTestConditions() {
        when(gumshieldApi.checklistApi()).thenReturn(checklistApi);

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL),
                eq("goodguy@hotmail.com"))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL),
                eq("goodguy@yahoo.com"))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL),
                eq(null))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("hotmail.com"))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq("yahoo.com"))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.EMAIL_DOMAIN),
                eq(null))).thenThrow(new NotFoundHttpStatusException());

        when(checklistApi.findEntryByValue(eq(ApiChecklistType.BLACK), eq(ApiChecklistAttribute.HOST),
                eq("1.1.1.1"))).thenThrow(new NotFoundHttpStatusException());
    }

    @Configuration
    static class TestContext {
        @Bean
        public BlacklistFilterConfig filterConfig() throws Exception {
            return new BlacklistFilterConfig.Builder(State.ENABLED, 1, null, "ACCOUNT_HOLDER",
                    false, 10, 10, GumtreeCustomHeaders.BUYER_GOOD.getHeaderValue(),
                    GumtreeCustomHeaders.SELLER_GOOD.getHeaderValue(), "")
                    .withStub(true)
                    .withExemptedCategories(exemptedCategories)
                    .withStubBlacklistedUsers(Arrays.asList("badguy@hotmail.com", "badguy@yahoo.com"))
                    .build();
        }

        @Bean
        public GumtreeBlacklistFilter filter(BlacklistFilterConfig filterConfig) {
            return new GumtreeBlacklistFilter()
                    .withPluginConfig(mock(Filter.class))
                    .withFilterConfig(filterConfig);
        }
    }
}
