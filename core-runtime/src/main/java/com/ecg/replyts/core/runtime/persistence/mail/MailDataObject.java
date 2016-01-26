package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.convert.RiakKey;

class MailDataObject {
    @RiakKey
    private String messageId;
    private byte[] mailData;
    private String bucketName;

    public MailDataObject(String messageId, byte[] mailData, String bucketName) { // NOSONAR
        this.messageId = messageId;
        this.mailData = mailData;
        this.bucketName = bucketName;
    }

    public String getMessageId() {
        return messageId;
    }

    public byte[] getMailData() {
        return mailData;
    }

    public String getBucketName() {
        return bucketName;
    }
}
