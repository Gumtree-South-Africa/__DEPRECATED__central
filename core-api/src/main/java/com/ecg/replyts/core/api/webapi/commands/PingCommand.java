package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.webapi.Method;

import java.util.Optional;

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
        return Optional.empty();
    }

}
