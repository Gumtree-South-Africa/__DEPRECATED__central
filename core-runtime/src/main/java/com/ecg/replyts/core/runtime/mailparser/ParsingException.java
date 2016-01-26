package com.ecg.replyts.core.runtime.mailparser;

/**
 * Indicates that the contents of the mail were so ill formatted, that the Mail parser was unable to understand it's contents. The mail is unprocessable therefore.
 */
public class ParsingException extends Exception {

    public ParsingException() {
    }

    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }

}
