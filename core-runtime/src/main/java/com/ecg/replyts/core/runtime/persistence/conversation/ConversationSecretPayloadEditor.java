package com.ecg.replyts.core.runtime.persistence.conversation;


import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;

/**
 * converts data for conversation secret repository into json and back into parseable information.
 *
 * @author mhuttar
 */
class ConversationSecretPayloadEditor {

    public String toJson(ConversationCreatedEvent event) {
        return JsonObjects.builder()
                .attr("ver", 1)
                .attr("bid", event.getBuyerId())
                .attr("sid", event.getSellerId())
                .attr("bs", event.getBuyerSecret())
                .attr("ss", event.getSellerSecret())
                .attr("adid", event.getAdId())
                .attr("ts", System.currentTimeMillis())
                .attr("cid", event.getConversationId()).toJson();
    }


    public NewConversationCommand fromJson(String json) {


        JsonNode input = JsonObjects.parse(json);

        String buyerId = input.get("bid").asText();
        String sellerId = input.get("sid").asText();
        String buyerSecret = input.get("bs").asText();
        String sellerSecret = input.get("ss").asText();
        String adid = input.get("adid").asText();
        String convId = input.get("cid").asText();

        return aNewConversationCommand(convId).
                withAdId(adid).
                withBuyer(buyerId, buyerSecret).
                withSeller(sellerId, sellerSecret).
                withCustomValues(Maps.<String, String>newHashMap()).
                build();
    }

}
