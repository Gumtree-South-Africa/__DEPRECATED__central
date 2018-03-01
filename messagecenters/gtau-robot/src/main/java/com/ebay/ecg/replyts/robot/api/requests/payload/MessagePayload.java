package com.ebay.ecg.replyts.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MessagePayload {

    private final String message;
    private final MessageDirection messageDirection;
    private final List<Link> links;
    private final MessageSender sender;
    private final RichMessage richTextMessage;

    public MessagePayload(String message, MessageDirection messageDirection, List<Entities.MessageLinkInfo> messageLinks, MessageSender sender, RichMessage richTextMessage) {
        this.message = message;
        this.messageDirection = messageDirection;
        this.links = messageLinks == null ? Collections.emptyList() : messageLinks.stream().map(Link::new).collect(Collectors.toList());
        this.sender = sender;
        this.richTextMessage = richTextMessage;
    }

    public String getMessage() {
        return message;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public List<Link> getLinks() {
        return links;
    }

    public MessageSender getSender() {
        return sender;
    }

    public RichMessage getRichTextMessage() {
        return richTextMessage;
    }
}
