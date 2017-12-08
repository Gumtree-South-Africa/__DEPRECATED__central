package com.ecg.replyts.core.api.persistence;

public interface HeldMailRepository {

    byte[] read(String messageId) throws MessageNotFoundException;

    void write(String messageId, byte[] content);

    void remove(String messageId);
}
