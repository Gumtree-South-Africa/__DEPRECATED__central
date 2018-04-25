package com.ecg.messagecenter.gtau.webapi;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;

import java.util.Properties;

import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.AD_ID;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.CUSTOM_FROM_USER_ID;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.CUSTOM_TO_USER_ID;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.FROM;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.MESSAGE;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.SUBJECT;
import static com.ecg.messagecenter.gtau.webapi.SyncTestBase.TestValues.TO;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;

public abstract class SyncTestBase {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(
            new Properties() {{
                put("replyts.tenant", "gtau");
                put("persistence.strategy", "cassandra");

                put("webapi.sync.au.enabled", "true");
                put("webapi.sync.v2.enabled", "true");
                put("webapi.diff.au.enabled", "true");
                put("messagebox.userid.by_user_id.customValueNameForBuyer", "buyer-user-id");
                put("messagebox.userid.by_user_id.customValueNameForSeller", "seller-user-id");
                put("messagebox.userid.userIdentifierStrategy", "BY_USER_ID");
            }},
            null, 20, ES_ENABLED,
            new Class[]{Object.class},
            "cassandra_schema.cql", "cassandra_messagebox_schema.cql", "cassandra_messagecenter_schema.cql");

    protected static MailBuilder buildMail() {
        return aNewMail()
                .from(FROM.value)
                .to(TO.value)
                .adId(AD_ID.value)
                .customHeader("buyer-user-id", CUSTOM_FROM_USER_ID.value)
                .customHeader("seller-user-id", CUSTOM_TO_USER_ID.value)
                .subject(SUBJECT.value)
                .plainBody(MESSAGE.value);
    }

    protected enum TestValues {
        FROM("from.mail@example.com"),
        TO("to.mail@example.com"),
        AD_ID("12345"),
        SUBJECT("Random subject"),
        MESSAGE("Just a string with text"),
        CUSTOM_FROM_USER_ID("user1"),
        CUSTOM_TO_USER_ID("user2");

        protected final String value;

        TestValues(String value) {
            this.value = value;
        }
    }
}
