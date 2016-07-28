package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

public class GetPostBoxCommand implements TypedCommand {

    // aargh: regex matching to bypass trailing email problem: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
    public static final String MAPPING = "/postboxes/{userId:.+}";

    private final String userId;

    public GetPostBoxCommand(String userId) {
        this.userId = userId;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/postboxes/" + userId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}
