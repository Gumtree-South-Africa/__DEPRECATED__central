package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.io.ByteStreams;
import org.openstack4j.model.storage.object.SwiftObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// This controller provides functionality similar to MailPartController but only for attachments
@Controller
@ConditionalOnProperty(name = "swift.attachment.storage.enabled", havingValue = "true")
public class AttachmentController {

    private final Timer readTimer = TimingReports.newTimer("attachmentController.read-attachment");

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentController.class);

    @Autowired
    private SwiftAttachmentRepository repository;

    AttachmentController() {
    }

    @ResponseBody
    @RequestMapping("/mail/{messageId}/attachments/{filename:.+}") /* {filename:.+} - need to have the regexp so that the file extension - e.g. .jpg does not get truncated */
    void retrieveAttachment(@PathVariable("messageId") String messageId, @PathVariable("filename") String filename,
                            HttpServletResponse response) {
        try (Timer.Context ignored = readTimer.time()) {
            repository.fetch(messageId, filename).ifPresent(attachment -> writeAttachment(response, attachment));
        }
    }

    private void writeAttachment(HttpServletResponse response, SwiftObject attachment) {
        LOG.debug("Writing attachment {}/{} size {} bytes to response stream", attachment.getDirectoryName(), attachment.getName(), attachment.getSizeInBytes());
        response.setContentType(attachment.getMimeType());
        response.setContentLengthLong(attachment.getSizeInBytes());
        try {
            ByteStreams.copy(attachment.download().getInputStream(), response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("failed to write the downloaded attachment, containerName=" + attachment.getContainerName()
                    + ", attachment=" + attachment.getDirectoryName() +"/"+ attachment.getName(), e);
        }
    }
}
