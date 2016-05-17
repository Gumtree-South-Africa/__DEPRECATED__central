package com.ecg.messagecenter.persistence;

public interface NewMessageListener {

    void success(String recipientUserId, long unreadCount, boolean isNewReply);
}
