package com.ecg.messagebox.controllers.responses;


import com.ecg.messagecenter.persistence.MessageType;
import com.ecg.messagecenter.util.MessageCenterUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class ResponseDataResponse {

    private List<ResponseData> responseData = new ArrayList<>();

    public ResponseDataResponse(List<com.ecg.messagecenter.persistence.ResponseData> responseDataList) {
        if (CollectionUtils.isNotEmpty(responseDataList)) {
            responseDataList.forEach(rd -> this.responseData.add(new ResponseData(rd)));
        }
    }

    public List<ResponseData> getResponseData() {
        return responseData;
    }

    class ResponseData {
        private String userId;
        private String conversationId;
        private int responseSpeed;
        private String conversationCreationDate;
        private MessageType conversationType;

        public ResponseData(com.ecg.messagecenter.persistence.ResponseData persistenceResponseData) {
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
    }

}
