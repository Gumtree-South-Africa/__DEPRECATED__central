package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.exception;

public class HttpRequestFailedException extends RuntimeException {

    public HttpRequestFailedException(String message) {
        super(message);
    }

    public HttpRequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
