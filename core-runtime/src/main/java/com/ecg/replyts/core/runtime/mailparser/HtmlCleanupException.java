package com.ecg.replyts.core.runtime.mailparser;

/**
 * Exception indicating that the progress of cleaning up html or removing all
 * html tags was not completed successfully. The original cause may be supplied
 * by the nested exception.
 *
 * @author huttar
 */
class HtmlCleanupException extends Exception {

    private static final long serialVersionUID = 1L;

    public HtmlCleanupException() {
        super();

    }

    public HtmlCleanupException(String message, Throwable cause) {
        super(message, cause);

    }

    public HtmlCleanupException(String message) {
        super(message);

    }

    public HtmlCleanupException(Throwable cause) {
        super(cause);

    }

}
