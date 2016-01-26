package com.ecg.replyts.core.api.model.mail;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Mail {
    /**
     * Mail header used to transport the ad id a conversation belongs to in the conversation starter mail
     */
    String ADID_HEADER = "X-ADID";
    String MESSAGE_ID_HEADER = "Message-ID";
    /**
     * Custom headers (indexed in elastic search, available for the full conversation) that cn be set in the conversation starter mail are expected to have this prefix.
     */
    String CUSTOM_HEADER_PREFIX = "X-CUST-";
    /**
     * The cloaked receiver mail address
     */
    String DELIVERED_TO_HEADER = "Delivered-To";
    String DISPOSITION_ATTACHMENT = "attachment";
    String IN_REPLY_TO_HEADER = "In-Reply-To";
    String REFERENCES_HEADER = "References";
    String FROM = "From";
    String TO = "To";
    String REPLY_TO = "Reply-To";
    String RECEIVED = "Received";

    /**
     * @return all headers by normalized header name, and values decoded according to RFC2047.
     */
    Map<String, List<String>> getDecodedHeaders();

    /**
     * @return the first value for all headers, the header name is normalized,
     * the values are decoded according to RFC2047.
     */
    Map<String, String> getUniqueHeaders();

    /**
     * @return values for the given header (case insensitive), decoded according to RFC2047
     */
    List<String> getHeaders(String name);

    /**
     * @return first value for the given header (case insensitive), decoded according to RFC2047, or null if not present
     */
    String getUniqueHeader(String headerName);

    /**
     * @return true if given header (case insensitive) is present
     */
    boolean containsHeader(String string);

    /**
     * @return the from e-mail address, or null when unparsable or not present
     */
    String getFrom();

    String getReplyTo();

    String getFromName();

    String getDeliveredTo();

    String getSubject();

    Date getSentDate();

    /**
     * @return parsed value of To header
     * @deprecated to get the actually address we should deliver to please use {@link #getDeliveredTo()}, this
     * method will return the value as it was put in by the sender, the DeliveredTo address is the envelope
     * address which is, security wise, more reliable.
     */
    @Deprecated
    List<String> getTo();

    String getMessageId();

    /**
     * @return the message id of the preceding message (derived from mail headers), or absent when not found
     */
    Optional<String> getLastReferencedMessageId();

    MediaType getMainContentType();

    String getAdId();

    Map<String, String> getCustomHeaders();

    /**
     * @return true, if this mail contains more than one body parts. Body parts can either be inline (multiple versions of the mail text - many mail clients will send mails in an html and a plaintext version at once) or any sort of attachments.
     */
    boolean isMultiPart();

    /**
     * @return true, if body parts with <code>Content-Disposition: attachment</code> are in the mail
     */
    boolean hasAttachments();

    /**
     * @return list of attachment's file names
     */
    List<String> getAttachmentNames();

    /**
     * @return a new mutable copy
     */
    MutableMail makeMutableCopy();

    /**
     * writes this mail to a given output stream.
     */
    void writeTo(OutputStream outputStream) throws IOException;

    /**
     * all body parts of the mail that are of mime type "text/*". if includeAttachments is false, then online inline parts are returned, also attachments are returned aswell.
     *
     * @param includeAttachments
     * @return
     */
    List<TypedContent<String>> getTextParts(boolean includeAttachments);

    /**
     * @return a list of strings that resemble the text parts of this mail. The first entry of this list will be the
     * default Body of the mail.
     */
    List<String> getPlaintextParts();

    /**
     * @return <code>true</code> if text parts of this mail can be modified. <code>false</code> if not
     */
    boolean isMutable();

    TypedContent<byte[]> getAttachment(String filename);
}
