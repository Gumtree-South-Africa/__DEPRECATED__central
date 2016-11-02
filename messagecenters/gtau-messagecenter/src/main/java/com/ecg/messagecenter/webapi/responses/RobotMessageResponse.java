package com.ecg.messagecenter.webapi.responses;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gafabic on 5/2/16.
 */
public class RobotMessageResponse {

    private RobotSender sender;
    private RichMessage richMessage;

    public RobotSender getSender() {
        return sender;
    }

    public void setSender(RobotSender sender) {
        this.sender = sender;
    }

    public RichMessage getRichMessage() {
        return richMessage;
    }

    public void setRichMessage(RichMessage richMessage) {
        this.richMessage = richMessage;
    }

    public static class RichMessage {
        private final String richMessage;
        private List<MessageResponse.MessageLink> messageLinks = new ArrayList<>();

        public RichMessage(String richMessage) {
            this.richMessage = richMessage;
        }

        public String getRichMessage() {
            return richMessage;
        }

        public List<MessageResponse.MessageLink> getMessageLinks() {
            return messageLinks;
        }

        public void setLinks(List<MessageResponse.MessageLink> links) {
            this.messageLinks = links;
        }
    }

    public static class RobotSender {
        private final String name;
        private final List<RobotSenderIcon> senderIcon = new ArrayList<>();

        public RobotSender(String name) {
            this.name = name;
        }

        public void addSenderIcon(String name, String url) {
            this.senderIcon.add(new RobotSenderIcon(name, url));
        }

        public List<RobotSenderIcon> getSenderIcon() {
            return senderIcon;
        }

        public String getName() {
            return name;
        }
    }

    public static class RobotSenderIcon {
        private final String name;
        private final String url;

        public RobotSenderIcon(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }
}
