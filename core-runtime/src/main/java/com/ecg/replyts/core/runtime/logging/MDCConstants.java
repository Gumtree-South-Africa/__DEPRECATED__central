package com.ecg.replyts.core.runtime.logging;

public class MDCConstants {

    private MDCConstants() {
        throw new AssertionError();
    }

    public static final String APPLICATION = "application";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String MAIL_BUYER = "mail_buyer";
    public static final String MAIL_FROM = "mail_from";
    public static final String MAIL_SELLER = "mail_seller";
    public static final String MAIL_TO = "mail_to";
    public static final String MESSAGE_ID = "message_id";
    public static final String REVISION = "revision";
    public static final String TENANT = "tenant";
}
