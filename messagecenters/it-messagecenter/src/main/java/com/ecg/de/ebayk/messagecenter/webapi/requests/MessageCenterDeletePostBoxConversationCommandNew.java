package com.ecg.de.ebayk.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:07
 *
 * @author maldana@ebay.de
 */
public class MessageCenterDeletePostBoxConversationCommandNew implements TypedCommand {

    public static final String MAPPING = "/postboxes/{email}/conversations";

    private final String email;

    public MessageCenterDeletePostBoxConversationCommandNew(String email) {
        this.email = email;
    }

    @Override public Method method() {
        return Method.DELETE;
    }

    @Override public String url() {
        return "/postboxes/" + email + "/conversations";
    }

    @Override public Optional<String> jsonPayload() {
        return Optional.absent();
    }

}
