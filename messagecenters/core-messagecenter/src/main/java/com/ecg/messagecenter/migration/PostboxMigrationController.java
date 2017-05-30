package com.ecg.messagecenter.migration;

import static com.ecg.replyts.core.webapi.control.ConversationMigrationController.*;


import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@ConditionalOnExpression("#{ '${persistence.strategy}'.startsWith('hybrid') }")
@RequestMapping("/msgmigration")
class PostboxMigrationController {

    private static final Logger LOG = LoggerFactory.getLogger(PostboxMigrationController.class);
    private volatile String status;

    @Autowired
    private ChunkedPostboxMigrationAction migrator;

    @RequestMapping("postboxes/{ids}")
    @ResponseBody
    public String migratePostboxes(@PathVariable String ids) {
        final List<String> postboxids = CSV_SPLITTER.splitToList(ids);

        LOG.info("Invoke index postboxes via Web interface for postboxes {} ", postboxids);
        boolean hasExecuted = migrator.migrateChunk(postboxids);
        if (hasExecuted) {
            status = String.format("Migrating postboxes %s started.", ids);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("postboxesBetween/{fromDate}/{toDate}")
    @ResponseBody
    public String migratePostboxesBetween(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate,
                                              @PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime toDate) {
        LOG.info("Invoke migratePostboxesBetween from {} to {} via web interface", fromDate, toDate);
        boolean hasExecuted = migrator.migratePostboxesBetween(fromDate, toDate);
        if (hasExecuted) {
            status = String.format("Migrating postboxes from %s to %s started.", fromDate, toDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }


    @RequestMapping("/postboxesFromDate/{fromDate}")
    @ResponseBody
    public String migratePostboxesFromDate(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate) {
        LOG.info("Invoke migratePostboxesFromDate via Web interface fromDate: {}", fromDate);
        boolean hasExecuted = migrator.migratePostboxesFromDate(fromDate);
        if (hasExecuted) {
            status = String.format("Migrating postboxes from %s started.", fromDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("allpostboxes")
    @ResponseBody
    public String migrateAllPostboxes() {
        LOG.info("Invoke migrateAllPostboxes via Web interface");
        boolean hasExecuted = migrator.migrateAllPostboxes();
        if (hasExecuted) {
            status = "Migrating all postboxes started.";
        }
        return hasExecuted? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("status")
    @ResponseBody
    public String getStatus() {
        LOG.info("Invoke getStatus via Web interface");
        if (status != null) {
            return String.format("Running: %s; " +
                    "Completed: %d%%; " +
                    "Processing rate: %s pbox/s; " +
                    "Expected completion in %d s; " +
                    "Time taken %ds; " +
                    "Postbox batches migrated %d, of %d total \n" +
                    "Total postboxes in the time slice %d \n</pre>",
                    status,
                    migrator.getPercentCompleted(),
                    migrator.getRatePostboxesPerSec(),
                    migrator.getExpectedCompletionTime(TimeUnit.SECONDS),
                    migrator.getTimeTaken(TimeUnit.SECONDS),
                    migrator.getPostboxBatchesMigrated(),
                    migrator.getTotalBatches(),
                    migrator.getTotalPostboxes());
        }
        return "Nothing has been executed";
    }


}
