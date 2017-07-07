package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;
import org.apache.http.HttpStatus;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

            Optional<SwiftObject> so = repository.fetch(messageId, filename);

            if (so.isPresent()) {
                writeAttachment(response, so.get());
            } else {
                response.setStatus(HttpStatus.SC_NOT_FOUND);
            }

        }
    }

    private void writeAttachment(HttpServletResponse response, SwiftObject attachment) {
        LOG.debug("Writing attachment {} size {} bytes to response stream", attachment.getName(), attachment.getSizeInBytes());
        response.setContentType(attachment.getMimeType());
        response.setContentLengthLong(attachment.getSizeInBytes());
        Stopwatch stopWatch = Stopwatch.createStarted();
        DLPayload payload = attachment.download();

        try (OutputStream outStream = response.getOutputStream();
             org.openstack4j.core.transport.HttpResponse resp = payload.getHttpResponse();
             InputStream inStream = payload.getInputStream()) {

            int swiftRespStatus = resp.getStatus();
            if (swiftRespStatus == HttpStatus.SC_OK) {
                ByteStreams.copy(inStream, outStream);
                outStream.flush();
            } else {
                LOG.error("Got {}/{} response from Swift, while attempting to fetch object {} from container {}, waited for {}ms ", swiftRespStatus, resp.getStatusMessage(),
                        attachment.getName(), attachment.getContainerName(), stopWatch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (IOException e) {
            String msg = String.format("Exception while downloading '%s' attachment from '%s' container, waited for %d ms",
                    attachment.getName(), attachment.getContainerName(), stopWatch.elapsed(TimeUnit.MILLISECONDS));
            throw new RuntimeException(msg, e);
        }
    }

    @ResponseBody
    @RequestMapping(value = "/mail/{messageId}/attachments", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    Map<String, SwiftObject> listAttachments(HttpServletResponse response, @PathVariable("messageId") String messageId) {

        Optional<Map<String, SwiftObject>> catalog = repository.getNames(messageId);
        if (catalog.isPresent()) {
            return catalog.get();
        } else {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
        }
        return new HashMap<>();
    }

}
