package com.ebay.ecg.replyts.robot.api.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

import static java.lang.String.format;

/**
 * Created by maotero on 15/09/2015.
 */
public class GetConversationsByAdIdAndEmailCommand implements TypedCommand {
    public static final String MAPPING = "/users/{email}/ads/{adId}";

    private String adId;
    private String email;

    public GetConversationsByAdIdAndEmailCommand(String email, String adId) {
        this.adId = adId;
        this.email = email;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return format("/users/%s/ads/%s",email,adId);
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}
