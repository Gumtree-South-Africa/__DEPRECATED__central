package com.ecg.replyts.core.api.sanitychecks;


/**
 * A message from a check contains an short description and a more detailed description.
 *
 * @author smoczarski
 */
public final class Message {

    private final String shortInfo;

    private final String details;

    private final Throwable cause;

    public static final Message EMPTY = new Message("", "", null);

    private Message(String shortInfo, String details, Throwable cause) {
        this.shortInfo = shortInfo;
        this.details = (details == null) ? "" : details;
        this.cause = cause;
    }

    /**
     * Creates an detailed message.
     *
     * @param shortInfo The short info.
     * @param details   The details.
     * @return The created message.
     */
    public static Message detailed(String shortInfo, String details) {
        return new Message(shortInfo, details, null);
    }

    /**
     * Creates a message from an exception.
     *
     * @param shortInfo The short info.
     * @param cause
     * @return The created message.
     */
    public static Message fromException(String shortInfo, Throwable cause) {
        return new Message(shortInfo, cause.getMessage(), cause);
    }

    /**
     * Creates a message from an exception.
     *
     * @param cause
     * @return The created message.
     */
    public static Message fromException(Throwable cause) {
        return new Message("", cause.getClass().getName() + " " + cause.getMessage(), cause);
    }

    /**
     * Creates a message only with a short info.
     *
     * @param shortInfo
     * @return The created message.
     */
    public static Message shortInfo(String shortInfo) {
        return new Message(shortInfo, "", null);
    }

    /**
     * @return The short info.
     */
    public String getShortInfo() {
        return shortInfo;
    }

    /**
     * @return A detailed info.
     */
    public String getDetails() {
        return details;
    }

    /**
     * @return The cause, if available.
     */
    public Throwable getCause() {
        return cause;
    }

    public boolean hasCause() {
        return cause != null;
    }

    @Override
    public String toString() {

        String separator = (hasText(shortInfo) && hasText(details)) ? ": " : "";
        return shortInfo + separator + details + (!hasCause() ? "" : "\n" + getStackTraceTop(cause));
    }

    private static boolean hasText(String input) {
        return input != null && !input.isEmpty();
    }

    private String getStackTraceTop(Throwable t) {
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackTrace = t.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) return "No Stack Trace";
        for (int i = 0; i < 10 && i < stackTrace.length; i++) {
            sb.append(stackTrace[i].toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((details == null) ? 0 : details.hashCode());
        result = prime * result + ((shortInfo == null) ? 0 : shortInfo.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Message)) {
            return false;
        }
        Message otherMessage = (Message) other;
        return shortInfo.equals(otherMessage.shortInfo) && details.equals(otherMessage.details);
    }

}
