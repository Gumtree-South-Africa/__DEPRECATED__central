package com.ecg.messagecenter.webapi.requests;

import com.ecg.messagecenter.migration.ChunkedPostboxMigrationAction;

import static com.ecg.replyts.core.webapi.control.ConversationMigrationController.*;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@ConditionalOnProperty(name = "r2cmigration", havingValue = "enabled")
@RequestMapping("/msgmigration")
class MigrationController {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationController.class);

    private String status;

    @Autowired
    private ChunkedPostboxMigrationAction migrator;

    @RequestMapping("postboxes/{ids}")
    @ResponseBody
    public String migratePostboxes(@PathVariable String ids) {
        final List<String> postboxids = CSV_SPLITTER.splitToList(ids);
        LOG.info("Invoke index postboxes via Web interface for postboxes {} ", postboxids);
        boolean hasExecuted = migrator.migrateChunk(postboxids);
        if (hasExecuted) {
            return String.format("Migrating postboxes %s started.", ids);
        }
        return DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("postboxesBetween/{fromDate}/{toDate}")
    @ResponseBody
    public String migratePostboxesBetween(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fromDate,
                                              @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime toDate) {
        LOG.info("Invoke migratePostboxesBetween from {} to {} via web interface", fromDate, toDate);
        boolean hasExecuted = migrator.migratePostboxesBetween(fromDate, toDate);
        if (hasExecuted) {
            return String.format("Migrating postboxes from %s to %s started.", fromDate, toDate);
        }
        return DEFAULT_NO_EXECUTION_MESSAGE;
    }


    @RequestMapping("/postboxesFromDate/{fromDate}")
    @ResponseBody
    public String migratePostboxesFromDate(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime fromDate) {
        LOG.info("Invoke migratePostboxesFromDate via Web interface fromDate: {}", fromDate);
        boolean hasExecuted = migrator.migratePostboxesFromDate(fromDate);
        if (hasExecuted) {
            return String.format("Migrating postboxes fromDate %s started.", fromDate);
        }
        return DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("allpostboxes")
    @ResponseBody
    public String migrateAllPostboxes() {
        LOG.info("Invoke migrateAllPostboxes via Web interface");
        boolean hasExecuted = migrator.migrateAllPostboxes();
        if (hasExecuted) {
            return "Migrating all postboxes started.";
        }
        return DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("status")
    @ResponseBody
    public String getStatus() {
        LOG.info("Invoke getStatus via Web interface");
        if (status != null) {
            return String.format("<pre>Running: %s \n" +
                    "Completed: %d%% \n" +
                    "Processing rate: %s conversations/per second \n" +
                    "Expected completion in %d s \n" +
                    "Avg number of conversations per time slice %d \n" +
                    "Time taken %ds \n" +
                    "Conversations migrated %d \n</pre>",
                    status,
                    migrator.getPercentCompleted(),
                    migrator.getRatePostboxesPerSec(),
                    migrator.getExpectedCompletionTime(TimeUnit.SECONDS),
                    migrator.getAvgPostboxesPerTimeSlice(),
                    migrator.getTimeTaken(TimeUnit.SECONDS),
                    migrator.getTotalPostboxesMigrated());
        }
        return "Nothing has been executed";
    }

}
