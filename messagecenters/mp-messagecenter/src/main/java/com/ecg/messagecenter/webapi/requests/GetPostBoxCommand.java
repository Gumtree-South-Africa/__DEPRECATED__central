package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

public class GetPostBoxCommand implements TypedCommand {

    // aargh: regex matching to bypass trailing email problem: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
    public static final String MAPPING = "/postboxes/{postBoxId:.+}";

    private final String postBoxId;

    public GetPostBoxCommand(String postBoxId) {
        this.postBoxId = postBoxId;
    }

    @Override
    public Method method() {
        return Method.GET;
    }

    @Override
    public String url() {
        return "/postboxes/" + postBoxId;
    }

    @Override
    public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}
