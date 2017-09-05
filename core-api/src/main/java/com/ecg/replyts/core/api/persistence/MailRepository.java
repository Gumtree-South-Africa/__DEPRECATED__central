package com.ecg.replyts.core.api.persistence;

import com.ecg.replyts.core.api.model.mail.Mail;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persists mails that belong to messages. Mails are actual E-Mail files without any further attached semantical
 * meaning.
 */
public interface MailRepository {

    /**
     * persists inbound and outbound mail (outbound only if available) under a specific message id
     */
    void persistMail(String messageId, byte[] mailData, Optional<byte[]> outgoingMailData);

    /**
     * returns input stream to an inbound mail identified by it's message id
     *
     * @param messageId identifier for inbound mail to load
     * @return input stream to mail data. if the mail could not be found, throws an IllegalStateException
     */
    byte[] readInboundMail(String messageId);

    /**
     * returns the parsed inbound mail
     */
    Mail readInboundMailParsed(String messageId);

    /**
     * returns input stream to an outbound mail identified by it's message id
     *
     * @param messageId identifier for outbound mail to load
     * @return input stream to mail data. if the mail could not be found, throws an IllegalStateException
     */
    byte[] readOutboundMail(String messageId);

    /**
     * returns the parsed outbound mail
     */
    Mail readOutboundMailParsed(String messageId);

    void deleteMailsByOlderThan(DateTime time, int maxResults, int numCleanUpThreads);

    /**
     * deletes inbound and outbound mails for this message id.
     */
    void deleteMail(String messageId);

    @Nonnull
    Stream<String> streamMailIdsSince(DateTime fromTime);

    @Nonnull
    Stream<String> streamMailIdsCreatedBetween(DateTime fromTime, DateTime toTime);

}
