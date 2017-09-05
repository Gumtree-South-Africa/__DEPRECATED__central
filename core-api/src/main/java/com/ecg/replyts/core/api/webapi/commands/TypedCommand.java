package com.ecg.replyts.core.api.webapi.commands;

import com.ecg.replyts.core.api.webapi.Method;

import java.util.Optional;

/**
 * Describes a call to one of ReplyTS webservices.
 *
 * @author huttar
 */
public interface TypedCommand {
    /**
     * Defines the HTTP Method to be used when invoking the webservice
     */
    Method method();

    /**
     * URL this command needs to be executed against
     */
    String url();

    /**
     * json payload as string to be transferred to the webservice. or an empty option if no payload required.
     */
    Optional<String> jsonPayload();

}
