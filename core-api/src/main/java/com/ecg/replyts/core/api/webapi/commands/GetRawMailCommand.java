package com.ecg.replyts.core.api.webapi.commands;


import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;

/**
 * Webservice Method Reference to Retrieve a complete Mail (identified by it's associated Message Id) in it's raw form
 * for download.
 *
 * @author huttar
 */
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
        return absent();
    }


}
