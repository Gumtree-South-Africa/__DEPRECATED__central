package com.ecg.messagecenter.kjca.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.webapi.control.Util.DEFAULT_NO_EXECUTION_MESSAGE;

@Controller
@ConditionalOnProperty(value = "persistence.strategy", havingValue = "hybrid")
@RequestMapping("/convblockmigration")
public class ConversationBlockMigrationController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockMigrationController.class);

    private String status;

    @Autowired
    private ConversationBlockMigration migrator;

    @RequestMapping("allconvblock")
    @ResponseBody
    public String migrateAllConversationBlocks() {
        LOG.info("Invoke migrateAllConversationBlocks via Web interface");
        boolean hasExecuted = migrator.migrateAllConversationBlocks();
        if (hasExecuted) {
            status = "Migrating all conversation block started.";
        }
        return hasExecuted ? status : DEFAULT_NO_EXECUTION_MESSAGE;
    }

    @RequestMapping("status")
    @ResponseBody
    public String getStatus() {
        LOG.info("Invoke getStatus via Web interface");
        if (status != null) {
            return String.format("<pre>" +
                            "Status: %s, " +
                            "Processing status: %s, " +
                            "Already migrated: %d, " +
                            "Time taken %dms" +
                            "</pre>",
                    status,
                    migrator.getProcessingStatus().toString(),
                    migrator.getMigratedCount(),
                    migrator.getTimeTaken(TimeUnit.MILLISECONDS));
        }
        return "Nothing has been executed";
    }
}
