package com.ecg.messagebox.controllers.responses;

import com.ecg.messagebox.model.MessageType;
import com.ecg.messagecenter.util.MessageCenterUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResponseDataResponse {

    private List<ResponseData> responseData = new ArrayList<>();

    public ResponseDataResponse(List<com.ecg.messagebox.model.ResponseData> responseDataList) {
        if (CollectionUtils.isNotEmpty(responseDataList)) {
            responseDataList.forEach(rd -> this.responseData.add(new ResponseData(rd)));
        }
    }

    public List<ResponseData> getResponseData() {
        return responseData;
    }

    public static class ResponseData {
        private String userId;
        private String conversationId;
        private int responseSpeed;
        private String conversationCreationDate;
        private MessageType conversationType;

        public ResponseData(com.ecg.messagebox.model.ResponseData persistenceResponseData) {
            this.userId = persistenceResponseData.getUserId();
            this.conversationId = persistenceResponseData.getConversationId();
            this.responseSpeed = persistenceResponseData.getResponseSpeed();
            this.conversationCreationDate = MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(persistenceResponseData.getConversationCreationDate());
            this.conversationType = persistenceResponseData.getConversationType();
        }

        public String getUserId() {
            return userId;
        }

        public String getConversationId() {
            return conversationId;
        }

        public int getResponseSpeed() {
            return responseSpeed;
        }

        public String getConversationCreationDate() {
            return conversationCreationDate;
        }

        public MessageType getConversationType() {
            return conversationType;
        }

        @Override
        public String toString() {
            return "ResponseData{" +
                    "userId='" + userId + '\'' +
                    ", conversationId='" + conversationId + '\'' +
                    ", responseSpeed=" + responseSpeed +
                    ", conversationCreationDate='" + conversationCreationDate + '\'' +
                    ", conversationType=" + conversationType +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResponseData that = (ResponseData) o;
            return responseSpeed == that.responseSpeed &&
                    Objects.equals(userId, that.userId) &&
                    Objects.equals(conversationId, that.conversationId) &&
                    Objects.equals(conversationCreationDate, that.conversationCreationDate) &&
                    conversationType == that.conversationType;

        }

        @Override
        public int hashCode() {
            int result = userId != null ? userId.hashCode() : 0;
            result = 31 * result + (conversationId != null ? conversationId.hashCode() : 0);
            result = 31 * result + responseSpeed;
            result = 31 * result + (conversationCreationDate != null ? conversationCreationDate.hashCode() : 0);
            result = 31 * result + (conversationType != null ? conversationType.hashCode() : 0);
            return result;
        }
    }

}
