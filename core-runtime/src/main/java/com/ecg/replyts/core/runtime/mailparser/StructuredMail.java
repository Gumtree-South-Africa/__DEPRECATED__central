package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.mailfixers.ContentTransferEncodingMultipartFix;
import com.ecg.replyts.core.runtime.mailfixers.ContentTypeBoundaryFix;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.MediaType;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.SingleBody;

import javax.mail.internet.InternetAddress;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.runtime.mailparser.MimeHelper.*;
import static com.google.common.base.Optional.fromNullable;
import static java.lang.String.format;

/**
 * Default implementation for {@link Mail} and {@link MutableMail}.
 */
public class StructuredMail implements Mail {


    public static final ContentTransferEncodingMultipartFix CONTENT_TRANSFER_ENCODING_MULTIPART_FIX = new ContentTransferEncodingMultipartFix();
    public static final LastReferenceMessageIdExtractor LAST_REFERENCE_MESSAGE_ID_EXTRACTOR = new LastReferenceMessageIdExtractor();
    public static final ContentTypeBoundaryFix CONTENT_TYPE_BOUNDARY_FIX = new ContentTypeBoundaryFix();

    private final Message mail;

    private final StructuredMailHeader mailHeader;
    private final MailBodyVisitingClient mailBodyVisitingClient;

    public StructuredMail(Message mail) {
        this.mail = mail;
        if (mail.isMultipart()) {
            // Some webmail clients generate the Content-Transfer-Encoding field wrong in multiparts.
            // we need to fix this, otherwise Mime4J will not be able to generate the outbound
            // mails correctly.
            CONTENT_TRANSFER_ENCODING_MULTIPART_FIX.applyIfNecessary(mail);

            // Outlook for Mac includes the sender's mangled (but readable) email address
            // in the multi-part boundary name. Rewrite the boundary name every time.
            CONTENT_TYPE_BOUNDARY_FIX.applyIfNecessary(mail);
        }
        mailBodyVisitingClient = new MailBodyVisitingClient(mail);

        mailHeader = new StructuredMailHeader(mail);
    }

    StructuredMailHeader getMailHeader() {
        return mailHeader;
    }

    MailBodyVisitingClient getMailBodyVisitingClient() {
        return mailBodyVisitingClient;
    }

    Message getOriginalMessage() {
        return mail;
    }

    public static Mail parseMail(InputStream i) throws ParsingException, IOException {
        try {
            StructuredMail structuredMail = new StructuredMail(parseAndConsume(i));
            // Mime4j will analyze the contents lazily. Do this now so that we know upfront when a mail is unparseable
            structuredMail.ensureMailIsParseable();
            return structuredMail;
        } catch (RuntimeException e) {
            throw new ParsingException("Mail could not be parsed", e);
        }
    }

    private void ensureMailIsParseable() throws ParsingException {

        TextBodyCharsetValidatingVisitor visitor = new TextBodyCharsetValidatingVisitor();
        try {
            // are the important mail addresses in this mail parseable?
            Preconditions.checkNotNull(getFrom(), "From field is empty or unparseable");

            mailBodyVisitingClient.visit(visitor);

        } catch (RuntimeException e) {
            throw new ParsingException(e.getMessage(), e);
        }

        // Text body charset okay?

        if (visitor.getEncodingErrors().isPresent()) {
            throw new ParsingException("Mail contains part in charset that is not understandable", visitor.getEncodingErrors().get());
        }
    }


    @Override
    public Map<String, List<String>> getDecodedHeaders() {
        return mailHeader.all();
    }

    @Override
    public Map<String, String> getUniqueHeaders() {
        return mailHeader.unique();
    }

    @Override
    public List<String> getHeaders(String name) {
        return mailHeader.list(name);
    }

    @Override
    public boolean containsHeader(String string) {
        return mailHeader.containsHeader(string);
    }

    @Override
    public String getUniqueHeader(String headerName) {
        List<String> headers = mailHeader.list(headerName);
        return headers.isEmpty() ? null : headers.get(0);
    }


    @Override
    public String getFrom() {
        Optional<InternetAddress> from = mailHeader.getHeaderAsInternetAddress(FROM);
        return from.isPresent() ? from.get().getAddress() : null;
    }

    @Override
    public String getReplyTo() {
        Optional<InternetAddress> replyTo = mailHeader.getHeaderAsInternetAddress(REPLY_TO);
        return replyTo.isPresent() ? replyTo.get().getAddress() : null;
    }

    @Override
    public String getFromName() {
        Optional<InternetAddress> replyTo = mailHeader.getHeaderAsInternetAddress(FROM);
        return replyTo.isPresent() ? replyTo.get().getPersonal() : null;
    }

    @Override
    public List<String> getTo() {
        return mailHeader.getMailAddressesFromHeader(TO);
    }

    @Override
    public String getDeliveredTo() {
        Optional<InternetAddress> deliveredTo = mailHeader.getHeaderAsInternetAddress(DELIVERED_TO_HEADER);
        return deliveredTo.isPresent() ? deliveredTo.get().getAddress() : null;
    }

    @Override
    public String getSubject() {
        return mail.getSubject();
    }

    @Override
    public Date getSentDate() {
        return mail.getDate();
    }

    @Override
    public String getMessageId() {
        return mail.getMessageId();
    }

    @Override
    public Optional<String> getLastReferencedMessageId() {
        return LAST_REFERENCE_MESSAGE_ID_EXTRACTOR.get(this);
    }

    @Override
    public String getAdId() {
        return getUniqueHeader(Mail.ADID_HEADER);
    }

    @Override
    public boolean isMultiPart() {
        return mail.isMultipart();
    }

    @Override
    public MediaType getMainContentType() {
        Optional<String> header = fromNullable(getUniqueHeader("Content-Type"));
        String filteredHeader = header.or("text/plain").trim();

        // some mail clients will have a semicolon at the end of the mediatype. this violates against the
        // media type format and causes MediaType.parse to crash

        String mediaType = filteredHeader.endsWith(";") ? filteredHeader.substring(0, filteredHeader.length()-1) : filteredHeader;

        try {
            return MediaType.parse(mediaType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getCustomHeaders() {
        return mailHeader.customHeaders();
    }

    @Override
    public boolean hasAttachments() {
        return mailBodyVisitingClient.visit(new AttachmentFindingVisitor()).hasAttachments();
    }

    @Override
    public List<String> getAttachmentNames() {
        return mailBodyVisitingClient.visit(new AttachmentFindingVisitor()).names();
    }

    @Override
    public MutableMail makeMutableCopy() {
        return new StructuredMutableMail(new StructuredMail(copy(mail)));
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        write(mail, outputStream);
    }

    @Override
    public List<TypedContent<String>> getTextParts(boolean includeAttachments) {
        return mailBodyVisitingClient.visit(new TextFindingVisitor(includeAttachments, false)).getContents();
    }

    @Override
    public List<String> getPlaintextParts() {
        return mailBodyVisitingClient.visit(new PlaintextExtractingVisitor()).getContents();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public TypedContent<byte[]> getAttachment(String filename) {
        AttachmentFindingVisitor visitor = new AttachmentFindingVisitor();
        mailBodyVisitingClient.visit(visitor);
        for (final Entity entity : visitor.getAttachments()) {
            if(filename.equals(entity.getFilename())) {
                SingleBody body = (SingleBody) entity.getBody();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    body.writeTo(os);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                return new TypedContent<byte[]>(MediaType.parse(entity.getMimeType()), os.toByteArray()) {
                    @Override
                    public boolean isMutable() {
                        return false;
                    }

                    @Override
                    public void overrideContent(byte[] newContent) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }
        throw new IllegalArgumentException(format("No such attachment found with file name '%s' for message '%s'", filename, mail.getMessageId()));
    }
}
