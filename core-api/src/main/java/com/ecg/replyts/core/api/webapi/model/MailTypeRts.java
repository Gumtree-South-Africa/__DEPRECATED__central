package com.ecg.replyts.core.api.webapi.model;

/**
 * Describes E-Mails (as can be retrieved by screening client). ReplyTS stores the inbound and the outbound version of
 * a mail. the inbound mail is as was received by ReplyTS, the outbound version is the anonymized (possibly modified)
 * version that ReplyTS sent out to the actual receiver.
 */
public enum MailTypeRts {
    /**
     * mail that was recieved by ReplyTS (unmodified)
     */
    INBOUND,
    /**
     * mail that was actually sent to the final recipient (modified, anonymized). Please note that only messages in state
     * SENT do have an outbound mail.
     */
    OUTBOUND
}
