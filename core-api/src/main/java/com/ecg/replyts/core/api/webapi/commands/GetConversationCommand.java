package com.ecg.replyts.core.api.webapi.commands;


import com.ecg.replyts.core.api.webapi.Method;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;

/**
 * Webservice Method Reference to Retrieve a complete Mail (identified by it's associated Message Id) in it's raw form
 * for download.
 *
 * @author huttar
 */
public class GetConversationCommand implements TypedCommand {

    public static final String MAPPING = "/conversation/{conversationId}";

    private final String conversationId;

    public GetConversationCommand(String conversationId) {
        this.conversationId = conversationId;
    }


    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/conversation/" + conversationId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return absent();
    }

}
