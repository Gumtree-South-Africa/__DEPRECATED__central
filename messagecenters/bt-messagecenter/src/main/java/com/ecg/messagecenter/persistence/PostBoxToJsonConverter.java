package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractPostBoxToJsonConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class PostBoxToJsonConverter implements AbstractPostBoxToJsonConverter<ConversationThread> {
    @Override
    public String toJson(PostBox p) {
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
            if (thread.getMessageDirection().isPresent()) {
                builder.attr("messageDirection", thread.getMessageDirection().get());
            }
            
            if (thread.getConversationState().isPresent()) {
                builder.attr("conversationState", thread.getConversationState().get().name());
            }
            
            if (thread.getCloseBy().isPresent()) {
                builder.attr("closeBy", thread.getCloseBy().get().name());
            }
            
            if (thread.getCustomValues().isPresent()) {
            	Map<String, String> customValues = thread.getCustomValues().get();

            	builder.attr("customValue", customValues);
            }

            threads.add(builder.build());
        }

        return threads;
    }

    private Long nullSafeMillis(DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.getMillis();
    }
}