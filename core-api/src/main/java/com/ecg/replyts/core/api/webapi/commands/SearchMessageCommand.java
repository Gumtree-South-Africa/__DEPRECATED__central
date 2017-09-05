package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class SearchMessageCommand implements TypedCommand {

    public static final String MAPPING = "/message/search";
    private final SearchMessagePayload searchDescription;

    public SearchMessageCommand(SearchMessagePayload searchDescription) {
        checkNotNull(searchDescription);
        this.searchDescription = searchDescription;
    }

    @Override
    public Method method() {
        return Method.POST;
    }

    @Override
    public String url() {
        return "/message/search";
    }

    @Override
    public Optional<String> jsonPayload() {
        try {
            return Optional.of(JsonObjects.getObjectMapper().writeValueAsString(searchDescription));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
