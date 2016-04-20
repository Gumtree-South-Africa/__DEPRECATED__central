package com.ecg.replyts.core.api.processing;

import org.apache.james.mime4j.dom.Message;


/**
 * Implemented by classes that mutate MIME4J representations of email messages.
 */
public interface MessageFixer {

    /**
     * Attempts to fix the provided message.
     * <p>
     * Implementors can mutate the underlying representation, but should not
     * throw exceptions.
     * <p>
     * Implementors can check if the modification is necessary/applicable by
     * looking at the mail content or the original exception that triggered the
     * fixing process.
     *
     * @param mail              Underlying email message representation.
     * @param originalException Exception that triggered the fixing process
     */
    @SuppressWarnings("UnusedParameters")
    default void applyIfNecessary(Message mail, Exception originalException) {
        applyIfNecessary(mail);
    }

    /**
     * Just like {@link MessageFixer#applyIfNecessary(Message, Exception)}, but
     * doesn't care about the exception.
     *
     * @see MessageFixer#applyIfNecessary(Message, Exception)
     */
    void applyIfNecessary(Message mail);
}
