package com.ecg.replyts.core.runtime.listener;

import com.google.common.base.Optional;

/**
 * Called when mail is processed and ready to be published
 */
public interface MailPublisher {

    void publishMail(String messageId, byte[] incomingMailData, Optional<byte[]> outgoingMailData);
}
