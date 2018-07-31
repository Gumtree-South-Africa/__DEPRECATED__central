package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class SearchMessageGroupCommand implements TypedCommand {

    public static final String MAPPING = "/message/group-search";
    private final SearchMessageGroupPayload searchDescription;

    public SearchMessageGroupCommand(SearchMessageGroupPayload searchDescription) {
        checkNotNull(searchDescription);
        this.searchDescription = searchDescription;
    }

    @Override
    public Method method() {
        return Method.POST;
    }

    @Override
    public String url() {
        return MAPPING;
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
