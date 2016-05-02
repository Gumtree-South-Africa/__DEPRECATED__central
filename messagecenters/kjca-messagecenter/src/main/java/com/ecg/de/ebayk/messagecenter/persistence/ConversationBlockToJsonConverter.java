package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.replyts.core.api.util.JsonObjects;

import static com.ecg.de.ebayk.messagecenter.util.ConverterUtils.nullSafeMillis;

public class ConversationBlockToJsonConverter {
    public String toJson(ConversationBlock conversationBlock) {
        JsonObjects.Builder builder = JsonObjects.builder()
                .attr("version", conversationBlock.getVersion());

        if (conversationBlock.getBuyerBlockedSellerAt().isPresent()) {
            builder.attr("buyerBlockedSellerAt", nullSafeMillis(conversationBlock.getBuyerBlockedSellerAt().get()));
        }
        if (conversationBlock.getSellerBlockedBuyerAt().isPresent()) {
            builder.attr("sellerBlockerBuyerAt", nullSafeMillis(conversationBlock.getSellerBlockedBuyerAt().get()));
        }

        return builder.toJson();
    }
}
