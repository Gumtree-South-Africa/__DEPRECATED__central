package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.de.ebayk.messagecenter.util.ConverterUtils;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

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
