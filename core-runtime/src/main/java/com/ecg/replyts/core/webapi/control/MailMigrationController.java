package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.runtime.migrator.MailAttachmentMigrator;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.webapi.control.Util.CSV_SPLITTER;
import static com.ecg.replyts.core.webapi.control.Util.DEFAULT_NO_EXECUTION_MESSAGE;

@Controller
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') && '${swift.attachment.storage.enabled:false}'.equalsIgnoreCase('true') }")
@RequestMapping("/mailmigration")
public class MailMigrationController {

    public static final String DATETIME_STRING = "dd-MM-yyyy'T'HH:mm";

    private static final Logger LOG = LoggerFactory.getLogger(MailMigrationController.class);

    private String status;

    @Autowired
    private MailAttachmentMigrator migrator;

    @RequestMapping(value = "mail/{ids}")
    @ResponseBody
    public String migratebyId(@PathVariable String ids) {
        final List<String> conversations = CSV_SPLITTER.splitToList(ids);
        LOG.info("Invoke migrate mail via Web interface for ids {} ", ids);
        boolean hasExecuted = migrator.migrateById(conversations);
        if (hasExecuted) {
            status = String.format("Migrating mails %s started.", ids);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping(value = "byid", method = RequestMethod.POST)
    @ResponseBody
    public String migratebyIds(@RequestBody String ids) {
        String cleanIds = "";
        try {
            cleanIds = java.net.URLDecoder.decode(ids, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }

        LOG.debug("Invoke migrate mail via Web interface for ids in a file '{}' ", cleanIds);

        final List<String> conversations = CSV_SPLITTER.splitToList(cleanIds);
        LOG.debug("Accepting {} ids for migration", conversations.size());
        boolean hasExecuted = migrator.migrateById(conversations);

        if (hasExecuted) {
            status = String.format("Migrating mails by id started.");
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("mailsBetween/{fromDate}/{toDate}")
    @ResponseBody
    public String migrateBetween(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate,
                                 @PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime toDate) {
        LOG.info("Invoke migrate from {} to {} via web interface", fromDate, toDate);
        boolean hasExecuted = migrator.migrate(fromDate, toDate);
        if (hasExecuted) {
            status = String.format("Migrating mails from %s to %s started.", fromDate, toDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("/mailsFromDate/{fromDate}")
    @ResponseBody
    public String migrateFromDate(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate) {
        LOG.info("Invoke migrateFromDate via Web interface for fromDate: {}", fromDate);
        boolean hasExecuted = migrator.migrateFromDate(fromDate);
        if (hasExecuted) {
            status = String.format("Migrating mail fromDate %s started.", fromDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("allmails")
    @ResponseBody
    public String migrateAll() {
        LOG.info("Invoke migrateAll via Web interface");
        boolean hasExecuted = migrator.migrateAll();
        if (hasExecuted) {
            status = "Migrating all mail started.";
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("status")
    @ResponseBody
    public String getStatus() {
        LOG.info("Invoke getStatus via Web interface");
        if (status != null) {
            return String.format("<pre>Running: %s \n" +
                            "Processing rate: %s (mails/s)\n" +
                            "Time taken %ds \n" +
                            "Batches migrated %d \n" +
                            " \n</pre>",
                    status,
                    migrator.getRateMailsPerSec(),
                    migrator.getTimeTaken(TimeUnit.SECONDS),
                    migrator.getBatchesMigrated(),
                    migrator.getBatchesMigrated());
        }
        return "Nothing has been executed";
    }

}
