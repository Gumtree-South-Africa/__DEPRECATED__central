package com.ecg.replyts.core.runtime.maildelivery;

/**
 * Exception that indicates that a mail could not be delivered by a {@link MailDeliveryService} implementation.
 *
 * @author huttar
 */
public class MailDeliveryException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -5922790696507692534L;

    public MailDeliveryException() {
        super();
    }

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(Throwable cause) {
        super(cause);
    }

}
