package com.ecg.replyts.core.api.model.conversation.event;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * A Conversation Event extended with information from the Conversation.
 *
 * This class is used by the event publisher plugins (e.g. to Kafka and RabbitMQ), and the consumers of those events.
 *
 * To deserialize using Jackson use code that is similar to:
 *
 * <pre>{@code
 * ObjectMapper objectMapper = new ObjectMapper();
 * objectMapper.registerModule(new JodaModule());
 *
 * ExtendedConversationEvent conversationEvent = objectMapper
 *     .reader(ExtendedConversationEvent.class)
 *     .readValue(input);
 * }
 * </pre>
 *
 */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtendedConversationEvent {

    public final ConversationEvent event;
    public final PublishedMessageConversation conversation;

    /**
     * Marks the version of this data format.
     */
    @JsonProperty(required = false)
    private int formatVer = 1;

    @JsonCreator
    public ExtendedConversationEvent(
            @JsonProperty("event") ConversationEvent event,
            @JsonProperty("conversation") PublishedMessageConversation conversation
    ) {
        this.event = event;
        this.conversation = conversation;
    }

    public ExtendedConversationEvent(Conversation conversation, ConversationEvent event, String sellerAnonymousEmail, String buyerAnonymousEmail) {
        this.event = event;
        this.conversation = new PublishedMessageConversation(conversation, sellerAnonymousEmail, buyerAnonymousEmail);
    }

    public static class PublishedMessageConversation {
        public final String sellerAnonymousEmail;
        public final String buyerAnonymousEmail;
        public final String conversationId;
        public final String adId;
        public final String buyerId;
        public final String sellerId;
        public final DateTime createdAt;
        public final DateTime lastModifiedAt;
        public final String state;
        public final boolean closed;
        public final boolean closedByBuyer;
        public final boolean closedBySeller;
        public final int messageCount;
        public final Map<String, String> customValues;

        @JsonCreator
        public PublishedMessageConversation(
                @JsonProperty("sellerAnonymousEmail,") String sellerAnonymousEmail,
                @JsonProperty("buyerAnonymousEmail,") String buyerAnonymousEmail,
                @JsonProperty("conversationId,") String conversationId,
                @JsonProperty("adId,") String adId,
                @JsonProperty("buyerId,") String buyerId,
                @JsonProperty("sellerId,") String sellerId,
                @JsonProperty("createdAt,") DateTime createdAt,
                @JsonProperty("lastModifiedAt,") DateTime lastModifiedAt,
                @JsonProperty("state,") String state,
                @JsonProperty("closed,") boolean closed,
                @JsonProperty("closedByBuyer,") boolean closedByBuyer,
                @JsonProperty("closedBySeller,") boolean closedBySeller,
                @JsonProperty("messageCount,") int messageCount,
                @JsonProperty("customValues") Map<String, String> customValues
        ) {
            this.sellerAnonymousEmail = sellerAnonymousEmail;
            this.buyerAnonymousEmail = buyerAnonymousEmail;
            this.conversationId = conversationId;
            this.adId = adId;
            this.buyerId = buyerId;
            this.sellerId = sellerId;
            this.createdAt = createdAt;
            this.lastModifiedAt = lastModifiedAt;
            this.state = state;
            this.closed = closed;
            this.closedByBuyer = closedByBuyer;
            this.closedBySeller = closedBySeller;
            this.messageCount = messageCount;
            this.customValues = customValues;
        }

        private PublishedMessageConversation(Conversation wrapped, String sellerAnonymousEmail, String buyerAnonymousEmail) {
            this.sellerAnonymousEmail = sellerAnonymousEmail;
            this.buyerAnonymousEmail = buyerAnonymousEmail;
            conversationId = wrapped.getId();
            adId = wrapped.getAdId();
            buyerId = wrapped.getBuyerId();
            sellerId = wrapped.getSellerId();
            createdAt = wrapped.getCreatedAt();
            lastModifiedAt = wrapped.getLastModifiedAt();
            state = wrapped.getState().toString();
            closedByBuyer = wrapped.isClosedBy(ConversationRole.Buyer);
            closedBySeller = wrapped.isClosedBy(ConversationRole.Seller);
            closed = closedByBuyer || closedBySeller;
            messageCount = wrapped.getMessages().size();
            // wrapped.getCustomValues() gives an immutable map, no need to copy it
            customValues = wrapped.getCustomValues();
        }
    }
}
