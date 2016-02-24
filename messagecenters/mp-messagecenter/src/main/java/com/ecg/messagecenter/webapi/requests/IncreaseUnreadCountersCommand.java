package com.ecg.messagecenter.webapi.requests;

import java.util.List;

public class IncreaseUnreadCountersCommand {

    public static final String MAPPING = "/postboxes/unread-counters";

    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item{
        private String userEmail;
        private String conversationId;
        private String message;

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public String getUserEmailLowerCase() {
            return userEmail.toLowerCase();
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
