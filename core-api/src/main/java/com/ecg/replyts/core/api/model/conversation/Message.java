package com.ecg.replyts.core.api.model.conversation;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit of exchange inside a {@link Conversation}.
 * <p/>
 * Contains only meta data of the complete email which is stored separately. The full content can be obtained
 * through the DFS service.
 */
public interface Message {

    /* @return version of this message (how many times it was modified) */
    int getVersion();

    /**
     * @return id
     */
    String getId();

    /**
     * @return a {@link MessageDirection}
     */
    MessageDirection getMessageDirection();

    /**
     * @return current state of this message
     */
    MessageState getState();

    /**
     * Date this message was received at.
     * <p/>
     * Please note, that, in contrast to normal mail client, the date returned by this function is not taken from
     * the source mail. Instead, it is the time the mail was received by ReplyTS. (Modifying the Sent date in mail
     * headers is a popular way to push spam up/down in mail clients.)
     *
     * @return Reply T&S receive date
     */
    DateTime getReceivedAt();

    /**
     * @return the date, this message was last modified
     */
    DateTime getLastModifiedAt();

    /**
     * @return value from the "Message-ID" header or null for unparsable mails and mails
     * that do not have this header (note: all mails are required to have this header)
     */
    String getSenderMessageIdHeader();

    /**
     * @return message id of the message this message is a response to, or null when unknown
     */
    String getInResponseToMessageId();

    /**
     * @return status, decided by filters
     */
    FilterResultState getFilterResultState();

    /**
     * @return status, decided by humans (defaults to {@link ModerationResultState#UNCHECKED})
     */
    ModerationResultState getHumanResultState();

    /**
     * @return the most important headers of the message
     */
    Map<String, String> getHeaders();

    /**
     * @return the plain text body of the message
     */
    String getPlainTextBody();

    /**
     * @return list of all text parts from the email
     */
    List<String> getTextParts();

    /**
     * @deprecated this version of text extracting does not work as one would assume and generates junk in 80% of the cases.
     * @param conversation the conversation this message is part of (not null)
     * @return the plain text of this message as a diff against the 'previous' message, if the previous
     * message can not be found, this method returns the same value as {@link #getPlainTextBody()}.
     */
    @Deprecated
    String getPlainTextBodyDiff(Conversation conversation);

    /**
     * @return list of (human readable) feedback received from filters, pre-processors and post-processors
     */
    List<ProcessingFeedback> getProcessingFeedback();

    /** @return list of attachment's filenames. if no attachments available, then an empty list. */
    List<String> getAttachmentFilenames();

    Optional<String> getLastEditor();

    UUID getEventTimeUUID();
}