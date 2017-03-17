package com.ecg.de.ebayk.messagecenter.webapi.responses;

import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.webapi.FlaggedCustomValue;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static com.ecg.de.ebayk.messagecenter.webapi.ConversationCustomValue.AT_POSTFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class PostBoxListItemResponseTest {

    public static final String BUYER_EMAIL = "buyer@example.com";
    public static final String AD_ID = "100021450";
    public static final String CONVERSATION_ID = "2:i4rfzm:id61mjst";
    public static final String MESSAGE_PREVIEW = "message preview ...";
    public static final String BUYER_NAME = "Sam";
    public static final String SELLER_NAME = "Joe";
    public static final String SELLER_EMAIL = "seller@example.com";
    public static final String FLAGGED_SELLER = "2015-01-01T12:00:00Z";
    public static final String FLAGGED_BUYER  = "2015-02-01T12:00:00Z";
    public static final String DELETED_SELLER = "2015-03-01T12:00:00Z";
    public static final String DELETED_BUYER  = "2015-04-01T12:00:00Z";

    @Test
    public void retrieveInboundConversation() {
        PostBoxListItemResponse response = new PostBoxListItemResponse(
                BUYER_EMAIL,
                conversationThread(MessageDirection.BUYER_TO_SELLER,
                Optional.of(SELLER_EMAIL)),
                conversationWithCustomValues());
        assertThat(response.getAdId(), equalTo(AD_ID));
        assertThat(response.getBoundness(), equalTo(MailTypeRts.OUTBOUND));
        assertThat(response.getBuyerName(), equalTo(BUYER_NAME));
        assertThat(response.getEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getId(), equalTo(CONVERSATION_ID));
        assertThat(response.getRole(), equalTo(ConversationRole.Buyer));
        assertThat(response.getSellerName(), equalTo(SELLER_NAME));
        assertThat(response.getSenderEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getTextShortTrimmed(), equalTo(MESSAGE_PREVIEW));
        assertThat(response.getBuyerEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getFlaggedBuyer(), equalTo(FLAGGED_BUYER));
        assertThat(response.getFlaggedSeller(), equalTo(FLAGGED_SELLER));
        assertThat(response.getDeletedBuyer(), equalTo(DELETED_BUYER));
        assertThat(response.getDeletedSeller(), equalTo(DELETED_SELLER));
    }

    @Test
    public void retrieveOutboundConversation() {
        PostBoxListItemResponse response = new PostBoxListItemResponse(
                BUYER_EMAIL,
                conversationThread(MessageDirection.SELLER_TO_BUYER,
                Optional.of(SELLER_EMAIL)),
                conversationWithCustomValues());
        assertThat(response.getAdId(), equalTo(AD_ID));
        assertThat(response.getBoundness(), equalTo(MailTypeRts.INBOUND));
        assertThat(response.getBuyerName(), equalTo(BUYER_NAME));
        assertThat(response.getEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getId(), equalTo(CONVERSATION_ID));
        assertThat(response.getRole(), equalTo(ConversationRole.Buyer));
        assertThat(response.getSellerName(), equalTo(SELLER_NAME));
        assertThat(response.getSenderEmail(), equalTo(SELLER_EMAIL));
        assertThat(response.getTextShortTrimmed(), equalTo(MESSAGE_PREVIEW));
        assertThat(response.getBuyerEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getFlaggedBuyer(), equalTo(FLAGGED_BUYER));
        assertThat(response.getFlaggedSeller(), equalTo(FLAGGED_SELLER));
        assertThat(response.getDeletedBuyer(), equalTo(DELETED_BUYER));
        assertThat(response.getDeletedSeller(), equalTo(DELETED_SELLER));
    }

    @Test
    public void retrieveConversationEmptySender() {
        PostBoxListItemResponse response = new PostBoxListItemResponse(
                BUYER_EMAIL,
                conversationThread(MessageDirection.SELLER_TO_BUYER,
                Optional.absent()),
                conversationWithCustomValues());
        assertThat(response.getAdId(), equalTo(AD_ID));
        assertThat(response.getBoundness(), equalTo(MailTypeRts.INBOUND));
        assertThat(response.getBuyerName(), equalTo(BUYER_NAME));
        assertThat(response.getEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getId(), equalTo(CONVERSATION_ID));
        assertThat(response.getRole(), equalTo(ConversationRole.Buyer));
        assertThat(response.getSellerName(), equalTo(SELLER_NAME));
        assertThat(response.getSenderEmail(), equalTo(null));
        assertThat(response.getTextShortTrimmed(), equalTo(MESSAGE_PREVIEW));
        assertThat(response.getBuyerEmail(), equalTo(BUYER_EMAIL));
        assertThat(response.getFlaggedBuyer(), equalTo(FLAGGED_BUYER));
        assertThat(response.getFlaggedSeller(), equalTo(FLAGGED_SELLER));
        assertThat(response.getDeletedBuyer(), equalTo(DELETED_BUYER));
        assertThat(response.getDeletedSeller(), equalTo(DELETED_SELLER));
    }

    private ConversationThread conversationThread(MessageDirection messageDirection, Optional<String> seller) {
        DateTime conversationDate = DateTime.now();
        return new ConversationThread(
                AD_ID,
                CONVERSATION_ID,
                conversationDate,
                conversationDate,
                conversationDate,
                false,
                Optional.of(MESSAGE_PREVIEW),
                Optional.of(BUYER_NAME),
                Optional.of(SELLER_NAME),
                Optional.of(BUYER_EMAIL),
                seller,
                Optional.of(messageDirection.toString())
        );
    }

    private Conversation conversationWithCustomValues() {
        Map<String, String> customValues = Maps.newHashMap();
        customValues.put("flagged-buyer-at", FLAGGED_BUYER);
        customValues.put("flagged-seller-at", FLAGGED_SELLER);
        customValues.put("deleted-buyer-at", DELETED_BUYER);
        customValues.put("deleted-seller-at", DELETED_SELLER);
        Conversation conversation = mock(Conversation.class);
        when(conversation.getCustomValues()).thenReturn(customValues);
        return conversation;
    }
}