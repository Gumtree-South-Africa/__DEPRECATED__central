package com.ecg.de.ebayk.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;

public abstract class ConversationCustomValue {

    public static final String AT_POSTFIX = "-at";
    public static final String DATE_POSTFIX = "-date";

    private ConversationRole buyerOrSeller;
    private String value;
    private String postfix = "";

    public ConversationCustomValue(ConversationRole role, String postfix, String value) {
        this.buyerOrSeller = role;
        this.postfix = postfix;
        this.value = value;
    }

    abstract String prefix();

    public String keyName() {
        return prefix() + buyerOrSeller.toString().toLowerCase() + postfix;
    }

    public String value() {
        return this.value;
    }

}
