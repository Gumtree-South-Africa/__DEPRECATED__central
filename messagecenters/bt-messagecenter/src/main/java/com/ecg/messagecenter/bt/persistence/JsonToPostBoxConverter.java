package com.ecg.messagecenter.bt.persistence;

import com.ecg.messagecenter.persistence.simple.AbstractJsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')}")
public class JsonToPostBoxConverter implements AbstractJsonToPostBoxConverter<ConversationThread> {
    @Override
    public PostBox toPostBox(String key, String jsonString) {
        List<ConversationThread> threadList = Lists.newArrayList();

        JsonNode json = JsonObjects.parse(jsonString);
        ArrayNode arrayNode = (ArrayNode) json.get("threads");

        for (JsonNode threadNode : arrayNode) {
            Map<String, String> customValues = new HashMap<>();

            if (threadNode.has("customValue")) {
                JsonNode customValueNodes = threadNode.get("customValue");

                Iterator<Map.Entry<String,JsonNode>> it= customValueNodes.fields();
                while(it.hasNext()) {
                    Map.Entry<String,JsonNode> item = it.next();

                    customValues.put(item.getKey(), item.getValue().asText());
                }
            }
            
            Optional<ConversationState> conversationState = threadNode.has("conversationState") ?
              Optional.of(ConversationState.valueOf(threadNode.get("conversationState").asText())) : Optional.empty();
            Optional<ConversationRole> closeBy = threadNode.has("closeBy") ?
              Optional.of(ConversationRole.valueOf(threadNode.get("closeBy").asText())) : Optional.empty();

            threadList.add(new ConversationThread(
              threadNode.get("adId").asText(),
              threadNode.get("conversationId").asText(),
              parseDate(threadNode, "createdAt"),
              parseDate(threadNode, "modifiedAt"),
              parseDate(threadNode, "receivedAt"),
              threadNode.get("containsUnreadMessages").asBoolean(),
              lookupStringValue(threadNode, "previewLastMessage"),
              lookupStringValue(threadNode,"buyerName"),
              lookupStringValue(threadNode,"sellerName"),
              lookupStringValue(threadNode, "buyerId"),
              lookupStringValue(threadNode, "messageDirection"),
              conversationState,
              closeBy,
              Optional.of(customValues)));
        }

        return new PostBox(key, lookupLongValue(json, "newRepliesCounter"), threadList);
    }

    private Optional<String> lookupStringValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asText()) : Optional.empty();
    }

    private Optional<Long> lookupLongValue(JsonNode threadNode, String key) {
        return threadNode.has(key) ? Optional.of(threadNode.get(key).asLong()) : Optional.empty();
    }

    private DateTime parseDate(JsonNode obj, String key) {
        JsonNode jsonNode = obj.get(key);

        if (jsonNode == null) {
            return null;
        }

        return DateTime.now().withMillis(jsonNode.asLong());
    }
}