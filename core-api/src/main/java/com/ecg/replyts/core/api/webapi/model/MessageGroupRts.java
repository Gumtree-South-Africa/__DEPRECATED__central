package com.ecg.replyts.core.api.webapi.model;

import com.ecg.replyts.core.api.webapi.envelope.PaginationInfo;

import java.util.List;

/**
 * Information about a group of messages returned from a ReplyTS search
 */
public interface MessageGroupRts {
    String getKey();
    PaginationInfo getPagination();
    List<MessageRts> getMessages();
}
