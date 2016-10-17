package com.ecg.messagebox.controllers;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;

import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.core.runtime.identifier.UserIdentifierType.BY_USER_ID;

public class ReplyTsIntegrationTestRuleHelper {

    private static final String CONFIG_DIR_PATH = "/mb-integration-test-conf";
    private static final String[] CQL_FILE_PATHS = new String[]{"cassandra_schema.cql", "cassandra_new_messagebox_schema.cql"};

    public static ReplyTsIntegrationTestRule getTestRuleForOldModel() {
        return new ReplyTsIntegrationTestRule(new Properties(getUserIdStrategyProperties()),
                CONFIG_DIR_PATH, CQL_FILE_PATHS);
    }

    public static ReplyTsIntegrationTestRule getTestRuleForNewModel() {
        return new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
            Properties properties = new Properties(getUserIdStrategyProperties());
            properties.put("messagebox.newModel.enabled.userIds", "1,2");
            properties.put("messagebox.useNewModel.userIds", "1,2");
            properties.put("messagebox.oldModel.enabled", "false");
            return properties;
        }).get(), CONFIG_DIR_PATH, CQL_FILE_PATHS);
    }

    private static Properties getUserIdStrategyProperties() {
        Properties properties = new Properties();
        properties.put("messagebox.userid.userIdentifierStrategy", BY_USER_ID.toString());
        properties.put("userIdentifierService", BY_USER_ID.toString());
        return properties;
    }
}