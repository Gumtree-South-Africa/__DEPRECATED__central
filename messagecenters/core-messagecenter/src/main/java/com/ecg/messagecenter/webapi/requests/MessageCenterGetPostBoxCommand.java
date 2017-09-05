package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;

import java.util.Optional;

public class MessageCenterGetPostBoxCommand implements TypedCommand {

    // aargh: regex matching to bypass trailing email problem: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
    public static final String MAPPING = "/postboxes/{email:.+}";

    private final String email;

    public MessageCenterGetPostBoxCommand(String email) {
        this.email = email;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/postboxes/" + email;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.empty();
    }
}
