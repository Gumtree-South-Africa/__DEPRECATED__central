package com.ebay.ecg.replyts.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class MessagePayload {

    private String message;
    private String messageDirection;
    private List<Link> links;
    private MessageSender sender;
    private RichMessage richTextMessage;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageDirection() {
        return messageDirection;
    }

    public void setMessageDirection(String messageDirection) {
        this.messageDirection = messageDirection;
    }

    public MessageDirection getMessageDirectionEnum() {
        if (messageDirection.equals(MessageDirection.BUYER_TO_SELLER.name())) {
            return MessageDirection.BUYER_TO_SELLER;
        } else if (messageDirection.equals(MessageDirection.SELLER_TO_BUYER.name())) {
            return MessageDirection.SELLER_TO_BUYER;
        } else {
            return MessageDirection.UNKNOWN;
        }
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void replaceLinks(List<Entities.MessageLinkInfo> messageLinks) {
        if (links == null) {
            links = Lists.newArrayList();
        } else {
            links.clear();
        }

        for (Entities.MessageLinkInfo link : messageLinks) {
            this.links.add(new Link(link.getUrl(), link.getType().name(), link.getBeginIndex(), link.getEndIndex()));
        }
    }

    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }

    public RichMessage getRichTextMessage() {
        return richTextMessage;
    }

    public void setRichTextMessage(RichMessage richTextMessage) {
        this.richTextMessage = richTextMessage;
    }

    public static class Link {
        private String url;
        private String type;
        private int start;
        private int end;

        public Link(String url, String type, int start, int end) {
            this.url = url;
            this.type = type;
            this.start = start;
            this.end = end;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class RichMessage {
        private String richMessageText;
        private List<Link> links = new ArrayList<>();

        public String getRichMessageText() {
            return richMessageText;
        }

        public void setRichMessageText(String richMessageText) {
            this.richMessageText = richMessageText;
        }

        public List<Link> getLinks() {
            return links;
        }

        public void replaceLinks(List<Entities.MessageLinkInfo> messageLinks) {
            if (links == null) {
                links = Lists.newArrayList();
            } else {
                links.clear();
            }

            for (Entities.MessageLinkInfo link : messageLinks) {
                this.links.add(new Link(link.getUrl(), link.getType().name(), link.getBeginIndex(), link.getEndIndex()));
            }
        }
    }
}
