package com.ecg.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;

public class FlaggedCustomValue extends ConversationCustomValue {
    public FlaggedCustomValue(ConversationRole role, String postfix, String value) {
        super(role, postfix, value);
    }

    public FlaggedCustomValue(ConversationRole role, String postfix) {
        super(role, postfix, null);
    }

    @Override
    protected String prefix() {
        return "flagged-";
    }
}