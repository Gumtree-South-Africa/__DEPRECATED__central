package com.ecg.replyts.core.api.webapi.commands;


import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;

import java.util.Optional;

public class GetRawMailCommand implements TypedCommand {

    public static final String MAPPING = "/mail/{messageId}/{mailType}";


    private final String messageId;
    private final MailTypeRts mailType;

    public GetRawMailCommand(String messageId, MailTypeRts mailType) {
        this.messageId = messageId;
        this.mailType = mailType;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/mail/" + messageId + "/" + mailType.name();
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }


}
