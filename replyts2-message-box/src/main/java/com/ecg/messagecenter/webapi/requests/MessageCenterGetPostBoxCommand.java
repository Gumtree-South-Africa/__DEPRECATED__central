package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:07
 *
 * @author maldana@ebay.de
 */
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
        return Optional.absent();
    }
}
