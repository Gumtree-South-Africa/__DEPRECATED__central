package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.exception;

public class IncorrectHttpStatusCodeException extends RuntimeException {

    public IncorrectHttpStatusCodeException(String message) {
        super(message);
    }

    public IncorrectHttpStatusCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
