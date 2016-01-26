package com.ecg.replyts.core.api.webapi.model.imp;

import com.ecg.replyts.core.api.webapi.envelope.PaginationInfo;
import com.ecg.replyts.core.api.webapi.model.MessageGroupRts;
import com.ecg.replyts.core.api.webapi.model.MessageRts;

import java.io.Serializable;
import java.util.List;

public class MessageGroupRtsRest implements MessageGroupRts, Serializable {
    private final String key;
    private final PaginationInfo pagination;
    private final List<MessageRts> messages;

    public MessageGroupRtsRest(String key, List<MessageRts> messages, PaginationInfo pagination) {
        this.key = key;
        this.messages = messages;
        this.pagination = pagination;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<MessageRts> getMessages() {
        return messages;
    }

    @Override
    public PaginationInfo getPagination() {
        return pagination;
    }
}
