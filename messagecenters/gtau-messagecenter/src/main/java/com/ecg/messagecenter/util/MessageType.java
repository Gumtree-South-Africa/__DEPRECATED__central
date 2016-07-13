package com.ecg.messagecenter.util;

import com.ecg.messagecenter.persistence.Header;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by mdarapour.
 */
public class MessageType {
    public static boolean isRobot(Message message) {
        return message.getHeaders().containsKey(Header.Robot.getValue());
    }
    public static boolean isOffer(Message message) {
        return message.getHeaders().containsKey(Header.OfferId.getValue());
    }
    public static boolean hasLinks(Message message) {
        return message.getHeaders().containsKey(Header.MessageLinks.getValue());
    }
    public static String getRobot(Message message) {
        return message.getHeaders().get(Header.Robot.getValue());
    }

    public static String getOffer(Message message) {
        return message.getHeaders().get(Header.OfferId.getValue());
    }

    public static List<MessageResponse.MessageLink> getLinks(Message message) {
        if(!hasLinks(message)) {
            return null;
        }

        JsonNode node = JsonObjects.parse(message.getHeaders().get(Header.MessageLinks.getValue()));
        if(!(node instanceof ArrayNode)) {
            return null;
        }

        ArrayNode array = ((ArrayNode) node);
        List<MessageResponse.MessageLink> links = Lists.newArrayList();
        for(JsonNode linkNode : array) {
            links.add(new MessageResponse.MessageLink(linkNode.get("start").asInt(),
                    linkNode.get("end").asInt(),
                    linkNode.get("type").asText(),
                    linkNode.get("url").asText()));
        }
        return links;
    }
}
