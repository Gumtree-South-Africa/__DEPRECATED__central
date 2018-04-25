package com.ecg.messagecenter.gtau.util;

import com.ecg.messagecenter.gtau.persistence.Header;
import com.ecg.messagecenter.gtau.webapi.responses.MessageResponse;
import com.ecg.messagecenter.gtau.webapi.responses.RobotMessageResponse;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

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

        return convertJsonToLinks(getHeader(message, Header.MessageLinks));
    }

    private static List<MessageResponse.MessageLink> convertJsonToLinks(String jsonString) {
        JsonNode node = JsonObjects.parse(jsonString);
        if (!(node instanceof ArrayNode)) {
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

    public static boolean isAutogate(Message message) {
        return message.getHeaders().containsKey(Header.Autogate.getValue());
    }

    public static Optional<RobotMessageResponse> getRobotDetails(Message message) {
        if (!isRobot(message)) {
            return Optional.empty();
        }

        final RobotMessageResponse robotResponse = new RobotMessageResponse();

        Optional<RobotMessageResponse.RobotSender> robotSender = getRobotSender(message);
        if (!robotSender.isPresent()) {
            return Optional.empty();
        }

        robotResponse.setSender(robotSender.get());
        Optional<RobotMessageResponse.RichMessage> richMessageOptional = getRichMessage(message);
        if (richMessageOptional.isPresent()) {
            robotResponse.setRichMessage(richMessageOptional.get());
        }

        return Optional.of(robotResponse);
    }

    private static Optional<RobotMessageResponse.RobotSender> getRobotSender(Message message) {
        if (!hasHeader(message, Header.MessageSender)) {
            return Optional.empty();
        }

        final JsonNode node = JsonObjects.parse(getHeader(message, Header.MessageSender));
        if (!node.has("name")) {
            return Optional.empty();
        }

        final RobotMessageResponse.RobotSender sender = new RobotMessageResponse.RobotSender(node.get("name").asText());
        if (node.has("senderIcons")) {
            JsonNode icons = node.get("senderIcons");
            if (icons instanceof ArrayNode) {
                for (JsonNode iconNode : icons) {
                    sender.addSenderIcon(iconNode.get("name").asText(), iconNode.get("url").asText());
                }
            }
        }

        return Optional.of(sender);
    }

    private static Optional<RobotMessageResponse.RichMessage> getRichMessage(Message message) {
        if (!hasHeader(message, Header.RichTextMessage)) {
            return Optional.empty();
        }

        final String richText = getHeader(message, Header.RichTextMessage);
        final RobotMessageResponse.RichMessage richMessage = new RobotMessageResponse.RichMessage(richText);

        if (hasHeader(message, Header.RichTextLinks)) {
            final List<MessageResponse.MessageLink> links = convertJsonToLinks(getHeader(message, Header.RichTextLinks));
            if (links != null) {
                richMessage.setLinks(links);
            }
        }

        return Optional.of(richMessage);
    }

    private static boolean hasHeader(Message message, Header header) {
        return message.getHeaders().containsKey(header.getValue());
    }

    private static String getHeader(Message message, Header header) {
        return message.getHeaders().get(header.getValue());
    }
}