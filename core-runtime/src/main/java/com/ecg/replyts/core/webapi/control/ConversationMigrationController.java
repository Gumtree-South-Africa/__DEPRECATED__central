package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.runtime.migrator.ChunkedConversationMigrationAction;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') }")
@RequestMapping("/coremigration")
public class ConversationMigrationController {

    public static final String DATETIME_STRING = "dd-MM-yyyy'T'HH:mm";

    private static final Logger LOG = LoggerFactory.getLogger(ConversationMigrationController.class);
    public static final String DEFAULT_NO_EXECUTION_MESSAGE = "No action was triggered as other process is already executing migration";
    public static final Splitter CSV_SPLITTER = Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(',')).or(CharMatcher.BREAKING_WHITESPACE)).trimResults().omitEmptyStrings();

    private String status;

    @Autowired
    private ChunkedConversationMigrationAction migrator;

    @RequestMapping("conversations/{ids}")
    @ResponseBody
    public String migrateConversations(@PathVariable String ids) {
        final List<String> conversations = CSV_SPLITTER.splitToList(ids);
        LOG.info("Invoke index conversation via Web interface using deep comparison for {} conversations with ids: {} ", conversations.size(), conversations);
        boolean hasExecuted = migrator.migrateChunk(conversations);
        if (hasExecuted) {
            status = String.format("Migrating conversations %s started.", ids);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("conversationsBetween/{fromDate}/{toDate}")
    @ResponseBody
    public String migrateConversationsBetween(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate,
                                              @PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime toDate) {
        LOG.info("Invoke migrateConversationsBetween from {} to {} via web interface", fromDate, toDate);
        boolean hasExecuted = migrator.migrateConversationsBetween(fromDate, toDate);
        if (hasExecuted) {
            status = String.format("Migrating conversations from %s to %s started.", fromDate, toDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("/conversationsFromDate/{fromDate}")
    @ResponseBody
    public String migrateConversationsFromDate(@PathVariable @DateTimeFormat(pattern = DATETIME_STRING) LocalDateTime fromDate) {
        LOG.info("Invoke migrateConversationsFromDate via Web interface fromDate: {}", fromDate);
        boolean hasExecuted = migrator.migrateConversationsFromDate(fromDate);
        if (hasExecuted) {
            status = String.format("Migrating conversations fromDate %s started.", fromDate);
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("allconversations")
    @ResponseBody
    public String migrateAllConversations() {
        LOG.info("Invoke migrateConversations via Web interface");
        boolean hasExecuted = migrator.migrateAllConversations();
        if (hasExecuted) {
            status = "Migrating all conversations started.";
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("status")
    @ResponseBody
    public String getStatus() {
        LOG.info("Invoke getStatus via Web interface");
        if (status != null) {
            return String.format("<pre>Running: %s \n" +
                            "Completed: %d%% \n" +
                            "Processing rate: %s \n" +
                            "Expected completion in %d s \n" +
                            "Time taken %ds \n" +
                            "Conversations batches migrated %d, of %d total \n" +
                            "Total conversations in the time slice %d \n</pre>",
                    status,
                    migrator.getPercentCompleted(),
                    migrator.getRateConversationsPerSec(),
                    migrator.getExpectedCompletionTime(TimeUnit.SECONDS),
                    migrator.getTimeTaken(TimeUnit.SECONDS),
                    migrator.getConversationsBatchesMigrated(),
                    migrator.getTotalBatches(),
                    migrator.getTotalConversations());
        }
        return "Nothing has been executed";
    }

}
