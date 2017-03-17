package com.ecg.de.ebayk.messagecenter.webapi;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;

public class DeletedCustomValue extends ConversationCustomValue {

    public DeletedCustomValue(ConversationRole role, String postfix, String value) {
        super(role, postfix, value);
    }

    public DeletedCustomValue(ConversationRole role, String postfix) {
        super(role, postfix, null);
    }

    @Override
    protected String prefix() {
        return "deleted-";
    }

}
