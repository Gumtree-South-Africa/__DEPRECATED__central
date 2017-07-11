package com.ecg.replyts.core.api.sanitychecks;

import static java.lang.String.format;

/**
 * Result output of a sanity check run
 *
 * @author smoczarski
 */
public final class Result {
    private final Status status;

    private final Message value;

    /**
     * Creates an successful result.
     *
     * @return The success result.
     */
    public static Result createResult(Status status, Message value) {
        if (status == null || value == null) {
            throw new IllegalArgumentException("status and result need to be available");
        }
        return new Result(status, value);
    }

    private Result(Status status, Message value) {
        this.status = status;
        this.value = value;
    }

    /**
     * @return True, if the processing was successful.
     */
    public Status status() {
        return status;
    }

    /**
     * @return An description in the case of an failed processing. Is only available if {@code #success} == false.
     */
    public Message value() {
        return value;
    }

    /**
     * @return True, if the result has a value.
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Format:<br>
     * successful ({value})<br>
     * failed ({value})
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        builder.append("status: ");
        builder.append(status);
        if (value != null) {
            builder.append(format(" (%s)", value));
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Result result = (Result) o;

        if (status != result.status) return false;
        if (!value.equals(result.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}