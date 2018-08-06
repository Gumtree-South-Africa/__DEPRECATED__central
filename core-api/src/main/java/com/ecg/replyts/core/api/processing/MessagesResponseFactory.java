package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;

public interface MessagesResponseFactory {

    String getCleanedMessage(Conversation conv, Message message);

}