package com.ecg.comaas.kjca.coremod.shared;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;

import java.net.SocketTimeoutException;

/**
 * This class contains exceptions raised while communicating with remote services, to provide more straightforward
 * handling.
 */
public final class RemoteServiceException extends RuntimeException {
    public enum Cause {
        HTTP,
        PARSE,
        UNKNOWN,
        TIMEOUT,
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT
    }

    private final Cause internalCause;

    RemoteServiceException(final Cause internalCause, String message, final Throwable cause) {
        super(message, cause);
        this.internalCause = internalCause;
    }

    RemoteServiceException(final int httpStatus, final String message, final Throwable cause) {
        super(message, cause);
        this.internalCause = mapExceptionByStatus(httpStatus);
    }

    private Cause mapExceptionByStatus(int status) {
        switch (status) {
            case HttpStatus.SC_BAD_REQUEST:
                return Cause.BAD_REQUEST;

            case HttpStatus.SC_FORBIDDEN:
                return Cause.FORBIDDEN;

            case HttpStatus.SC_NOT_FOUND:
                return Cause.NOT_FOUND;

            case HttpStatus.SC_CONFLICT:
                return Cause.CONFLICT;

            default:
                return Cause.HTTP;
        }
    }

    RemoteServiceException(final Throwable cause) {
        super(cause);
        this.internalCause = mapExceptionToType(cause);
    }

    private Cause mapExceptionToType(final Throwable exception) {
        final Class<? extends Throwable> clazz = exception.getClass();

        if (clazz == RemoteServiceException.class) {
            return ((RemoteServiceException) exception).getInternalCause();
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

        if (clazz == UnsupportedOperationException.class) {
            return Cause.HTTP;
        }

        return Cause.UNKNOWN;
    }

    public Cause getInternalCause() {
        return internalCause;
    }

    @Override
    public String toString() {
        return "RemoteServiceException{" +
                "internalCause=" + internalCause +
                "} " + super.toString();
    }
}
