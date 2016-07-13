package com.ecg.messagecenter.pushmessage.send.model;

import com.ecg.messagecenter.pushmessage.send.client.SendClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SendMessage {

    @JsonView(Views.Response.class)
    @JsonProperty("id")
    private String id;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("userId")
    private Long userId;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("type")
    private SendClient.NotificationType type;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("referenceId")
    private String referenceId;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("alertCounter")
    private Integer alertCounter;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("message")
    private String message;

    @JsonView({Views.Response.class, Views.Request.class})
    @JsonProperty("details")
    private Map<String, String> details;

    private SendMessage() {
        this(null, null, null, null, null, null);
    }

    private SendMessage(
            final Long userId,
            final SendClient.NotificationType type,
            final String referenceId,
            final Integer alertCounter,
            final String message,
            final Map<String, String> details
    ) {
        this.userId = userId;
        this.type = type;
        this.referenceId = referenceId;
        this.alertCounter = alertCounter;
        this.message = message;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public SendClient.NotificationType getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Integer getAlertCounter() {
        return alertCounter;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public static class MessageBodyBuilder {
        private Long userId = null;
        private SendClient.NotificationType type = null;
        private String referenceId = null;
        private Integer alertCounter = null;
        private String message = null;
        private Map<String, String> details = null;

        public MessageBodyBuilder setUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public MessageBodyBuilder setType(SendClient.NotificationType type) {
            this.type = type;
            return this;
        }

        public MessageBodyBuilder setReferenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public MessageBodyBuilder setAlertCounter(Integer alertCounter) {
            this.alertCounter = alertCounter;
            return this;
        }

        public MessageBodyBuilder setMessage(String message) {
            this.message = message;
            return this;
        }

        public MessageBodyBuilder setDetails(Map<String, String> details) {
            this.details = details;
            return this;
        }

        public SendMessage createMessageBody() {
            return new SendMessage(userId, type, referenceId, alertCounter, message, details);
        }
    }

    //classes for different jackson views.
    public static class Views {
        public static class Request {
        }

        public static class Response {
        }
    }

}
