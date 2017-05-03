package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractPostBoxToJsonConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class PostBoxToJsonConverter implements AbstractPostBoxToJsonConverter<ConversationThread> {
    @Override
    public String toJson(PostBox<ConversationThread> p) {
        return JsonObjects.builder()
          .attr("version", 1)
          .attr("newRepliesCounter", p.getNewRepliesCounter().getValue())
          .attr("threads", threads(p.getConversationThreads()))
          .toJson();
    }

    private ArrayNode threads(List<ConversationThread> conversationThreads) {
        ArrayNode threads = JsonObjects.newJsonArray();

        for (ConversationThread thread : conversationThreads) {
            JsonObjects.Builder builder = JsonObjects.builder()
              .attr("adId", thread.getAdId())
              .attr("createdAt", nullSafeMillis(thread.getCreatedAt()))
              .attr("modifiedAt", nullSafeMillis(thread.getModifiedAt()))
              .attr("conversationId", thread.getConversationId())
              .attr("containsUnreadMessages", thread.isContainsUnreadMessages());

            if (thread.getReceivedAt() != null) {
                builder.attr("receivedAt", thread.getReceivedAt().getMillis());
            }

            if (thread.getPreviewLastMessage().isPresent()) {
                builder.attr("previewLastMessage", thread.getPreviewLastMessage().get());
            }
            if (thread.getBuyerName().isPresent()) {
                builder.attr("buyerName", thread.getBuyerName().get());
            }
            if (thread.getSellerName().isPresent()) {
                builder.attr("sellerName", thread.getSellerName().get());
            }
            if (thread.getBuyerId().isPresent()) {
                builder.attr("buyerId", thread.getBuyerId().get());
            }
            if (thread.getSellerId().isPresent()) {
                builder.attr("sellerId", thread.getSellerId().get());
            }
            if (thread.getMessageDirection().isPresent()) {
                builder.attr("messageDirection", thread.getMessageDirection().get());
            }
            if (thread.getRobot().isPresent()) {
                builder.attr("robot", thread.getRobot().get());
            }
            if (thread.getOfferId().isPresent()) {
                builder.attr("offerId", thread.getOfferId().get());
            }
            if (CollectionUtils.isNotEmpty(thread.getLastMessageAttachments())) {
                builder.attr("lastMessageAttachments", attachments(thread.getLastMessageAttachments()));
            }
            if (thread.getLastMessageId().isPresent()) {
                builder.attr("lastMessageId", thread.getLastMessageId().get());
            }
            if (thread.getBuyerAnonymousEmail().isPresent()) {
                builder.attr("buyerAnonymousEmail", thread.getBuyerAnonymousEmail().get());
            }
            if (thread.getSellerAnonymousEmail().isPresent()) {
                builder.attr("sellerAnonymousEmail", thread.getSellerAnonymousEmail().get());
            }
            if (thread.getStatus().isPresent()) {
                builder.attr("status", thread.getStatus().get());
            }

            threads.add(builder.build());
        }

        return threads;
    }

    private ArrayNode attachments(List<String> lastMessageAttachments) {
        final ArrayNode attachments = JsonObjects.newJsonArray();
        for (String attachment : lastMessageAttachments) {
            attachments.add(attachment);
        }
        return attachments;
    }

    private Long nullSafeMillis(DateTime dateTime) {
        if (dateTime == null)
            return null;
        return dateTime.getMillis();
    }
}
