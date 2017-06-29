package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.runtime.migrator.MailAttachmentVerifier;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static com.ecg.replyts.core.webapi.control.MailMigrationController.DATETIME_STRING;
import static com.ecg.replyts.core.webapi.control.Util.CSV_SPLITTER;

@Controller
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') && '${swift.attachment.storage.enabled:false}'.equalsIgnoreCase('true') }")
@RequestMapping("/verifyattachment")
public class AttachmentVerifierController {

    private static final Logger LOG = LoggerFactory.getLogger(AttachmentVerifierController.class);

    @Autowired
    private MailAttachmentVerifier mailVerifier;

    @Autowired
    private ThreadPoolExecutor executor;

    @RequestMapping("/betweenDates/{fromDate}/{toDate}")
    @ResponseBody
    public String verifyBetween(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate,
                                @PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime toDate,
                                @RequestParam(name = "migrate", required = false, defaultValue = "false") boolean allowMigration) {
        LOG.info("Invoke attachment verifier from {} to {} via web interface", fromDate, toDate);

        try {
            executor.execute(() -> mailVerifier.verifyAttachmentsBetweenDates(fromDate, toDate, allowMigration));
            return "Attachment Verification is UP and RUNNING!";
        } catch (Exception e) {
            executor.getQueue().clear();
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/{ids}")
    @ResponseBody
    public String verifyIds(@PathVariable String ids,
                            @RequestParam(name = "migrate", required = false, defaultValue = "false") boolean allowMigration) {
        List<String> mailIds = CSV_SPLITTER.splitToList(ids);
        LOG.info("Invoke attachment verifier for IDs {} via web interface", mailIds);

        try {
            executor.execute(() -> mailVerifier.verifyAttachmentsByIds(mailIds, allowMigration));
            return "Attachment Verification is UP and RUNNING!";
        } catch (Exception e) {
            executor.getQueue().clear();
            throw new RuntimeException(e);
        }
    }
}
