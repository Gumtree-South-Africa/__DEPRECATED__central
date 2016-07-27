package com.ecg.messagecenter.persistence.block;

import com.ecg.messagecenter.util.ConverterUtils;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}' == 'hybrid'}")
public class JsonToConversationBlockConverter {
    public ConversationBlock toConversationBlock(String conversationId, String jsonString) {
        JsonNode json = JsonObjects.parse(jsonString);

        return new ConversationBlock(
                conversationId,
                json.get("version").asInt(1),
                Optional.ofNullable(ConverterUtils.parseDate(json, "buyerBlockedSellerAt")),
                Optional.ofNullable(ConverterUtils.parseDate(json, "sellerBlockerBuyerAt"))
                );
    }
}
