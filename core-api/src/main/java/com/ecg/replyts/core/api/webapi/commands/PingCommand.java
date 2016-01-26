package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.webapi.Method;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;

/**
 * Command that asks the server to output the input it gives to him
 *
 * @author huttar
 */
public class PingCommand implements TypedCommand {

    public static final String MAPPING = "/echo";

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return MAPPING;
    }

    @Override
    public Optional<String> jsonPayload() {
        return absent();
    }

}
