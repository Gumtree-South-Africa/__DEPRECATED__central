package com.ecg.messagebox.service;

import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;

import java.util.List;

public interface ResponseDataService {

    /**
     * Get the user response data
     * @param userId the user id
     * @return the list of response data per conversation
     */
    List<ResponseData> getResponseData(String userId);

    /**
     * Calculate the user response data for the new message of the conversation.
     * @param userId the user id
     * @param conversation the core conversation
     * @param message the core message
     */
    void calculateResponseData(String userId, Conversation conversation, Message message);
}
