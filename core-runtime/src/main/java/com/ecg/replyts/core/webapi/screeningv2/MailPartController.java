package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@ConditionalOnExpression("'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')")
public class MailPartController {
    private final MailRepository mailRepository;

    @Autowired
    MailPartController(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    @ResponseBody
    @RequestMapping(value = "/mail/{messageId}/{mailType}/parts", produces = MediaType.APPLICATION_JSON_VALUE)
    Object describeMail(@PathVariable("messageId") String messageId, @PathVariable("mailType") MailTypeRts type) {

        Mail mailContents = load(messageId, type);

        String contentType = null;
        String content = null;

        List<TypedContent<String>> textParts = mailContents.getTextParts(false);
        if (!textParts.isEmpty()) {
            contentType = textParts.get(0).getMediaType().toString();
            content = textParts.get(0).getContent();
        }
        return ResponseObject.of(new MailOverviewRts(mailContents.getUniqueHeaders(), content, contentType, mailContents.getAttachmentNames()));
    }

    @ResponseBody
    @RequestMapping("/mail/{messageId}/{mailType}/parts/{filename:.+}") /* {filename:.+} - need to have the regexp so that the file extension - e.g. .jpg does not get truncated */
    void retrieveAttachment(@PathVariable("messageId") String messageId, @PathVariable("mailType") MailTypeRts type, @PathVariable("filename") String filename, HttpServletResponse response) throws IOException {

        Mail mailContents = load(messageId, type);
        TypedContent<byte[]> attachment = mailContents.getAttachment(filename);

        response.setContentType(attachment.getMediaType().toString());
        response.setContentLength(attachment.getContent().length);
        response.getOutputStream().write(attachment.getContent());
    }


    private Mail load(String messageId, MailTypeRts type) {
        Mail mailContents;

        switch (type) {
            case OUTBOUND:
                mailContents = mailRepository.readOutboundMailParsed(messageId);
                break;
            case INBOUND:
            default:
                mailContents = mailRepository.readInboundMailParsed(messageId);
                break;
        }
        return mailContents;
    }

    class MailOverviewRts {
        private final Map<String, String> headers;
        private final Body content;
        private final List<String> attachments;

        MailOverviewRts(Map<String, String> headers, String contents, String contentType, List<String> attachments) {
            this.headers = headers;
            this.content = new Body(contentType, contents);
            this.attachments = attachments;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Body getContents() {
            return content;
        }

        public List<String> getAttachments() {
            return attachments;
        }

    }

    static class Body {
        private final String contentType;
        private final String content;

        public Body(String contentType, String contents) {
            this.contentType = contentType;
            this.content = contents;
        }

        public String getContentType() {
            return contentType;
        }

        public String getContent() {
            return content;
        }
    }


}
