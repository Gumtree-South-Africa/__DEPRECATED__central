package com.ecg.messagecenter.pushmessage.send.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;

import java.net.SocketTimeoutException;

/**
 * Container for wrapping downstream exceptions that arise while accessing SEND
 */
public class SendException extends RuntimeException {
    private final Cause internalCause;

    public SendException(final Cause internalCause, String message, final Throwable cause) {
        super(message, cause);
        this.internalCause = internalCause;
    }

    public SendException(final Throwable cause) {
        super(cause);
        this.internalCause = mapExceptionToType(cause);
    }

    private Cause mapExceptionToType(final Throwable exception) {
        final Class<? extends Throwable> clazz = exception.getClass();

        if (clazz == SendException.class) {
            return ((SendException) exception).getInternalCause();
        }

        if (clazz == ClientProtocolException.class) {
            return Cause.HTTP;
        }

        if (clazz == HttpHostConnectException.class || clazz == SocketTimeoutException.class) {
            return Cause.TIMEOUT;
        }

        if (clazz == JsonParseException.class || clazz == JsonMappingException.class) {
            return Cause.PARSE;
        }

        return Cause.UNKNOWN;
    }

    public Cause getInternalCause() {
        return internalCause;
    }

    public enum Cause {
        CONFLICT,
        HTTP,
        PARSE,
        UNKNOWN,
        TIMEOUT,
        DISCOVERY
    }
}
