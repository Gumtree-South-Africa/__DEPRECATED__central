package com.ebay.ecg.replyts.robot.api.requests.payload;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gafabic on 4/27/16.
 */
public class MessageSender {

    private String name;
    private List<SenderIcon> senderIcons = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SenderIcon> getSenderIcons() {
        return senderIcons;
    }

    public void addSenderIcon(String name, String url) {
        final SenderIcon senderIcon = new SenderIcon();
        senderIcon.setName(name);
        senderIcon.setUrl(url);
        senderIcons.add(senderIcon);
    }

    public static class SenderIcon {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
