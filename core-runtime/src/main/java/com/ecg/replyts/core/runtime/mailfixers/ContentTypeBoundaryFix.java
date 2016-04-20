package com.ecg.replyts.core.runtime.mailfixers;

import com.ecg.replyts.core.api.processing.MessageFixer;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.util.MimeUtil;

/**
 * In some email clients (like MS Outlook on Mac) the name of the multipart
 * boundary is generated from the sender's email address. For example,
 *
 * Content-Type: multipart/alternative;
 *   boundary="_000_D163B3112A4Edvassilenkoebaycom_"
 *
 * This class rewrites the boundary name with a randomly generated string to
 * avoid exposing the email address.
 */
public class ContentTypeBoundaryFix implements MessageFixer {
    @Override
    public void applyIfNecessary(Message mail) {
        ContentTypeField contentType = (ContentTypeField) mail.getHeader().getField(FieldName.CONTENT_TYPE);

        if (contentType != null && contentType.isMultipart()) {
            String oldBoundary = contentType.getBoundary();
            String newBoundary = '"' + MimeUtil.createUniqueBoundary() + '"';
            String newContentTypeBody = contentType.getBody()
                    .replace('"' + oldBoundary + '"', newBoundary)
                    .replace(oldBoundary, newBoundary);
            ContentTypeField newContentType = Fields.contentType(newContentTypeBody);
            mail.getHeader().setField(newContentType);
        }
    }
}
