package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

public class BlockedUserFilterIntegrationTest {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/comaas";
    private static final String DB_USER = "mysql_guest";
    private static final String DB_PASS = "mysql_guest";
    private static final String FROM_EMAIL = "buyer@example.com";

    private final Properties testProperties = new Properties() {{
        put("replyts2-belenblockeduserfilter-plugin.dataSource.url", DB_URL);
        put("replyts2-belenblockeduserfilter-plugin.dataSource.username", DB_USER);
        put("replyts2-belenblockeduserfilter-plugin.dataSource.password", DB_PASS);
        put("replyts2-belenblockeduserfilter-plugin.dataSource.maxPoolSize", 10);
    }};

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(testProperties);

    private JdbcTemplate template;

    @Before
    public void setUp() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(DB_URL);
        dataSource.setUsername(DB_USER);
        dataSource.setPassword(DB_PASS);
        template = new JdbcTemplate(dataSource);
    }

    @Test
    public void whenUserDataActive_shouldSendMail() {
        template.execute("INSERT INTO userdata (status, email) VALUES ('active', '" + FROM_EMAIL + "')");
        testRule.registerConfig(BlockedUserFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.waitForMail();
    }

    @Test
    public void whenUserDataBlocked_shouldDropMail() {
        template.execute("INSERT INTO userdata (status, email) VALUES ('blocked', '" + FROM_EMAIL + "')");
        testRule.registerConfig(BlockedUserFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.assertNoMailArrives();
    }

    @Test
    public void whenExtTnsActive_shouldSendMail() {
        template.execute("INSERT INTO external_user_tns (status, email) VALUES ('active', '" + FROM_EMAIL + "')");
        testRule.registerConfig(BlockedUserFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.waitForMail();
    }

    @Test
    public void whenExtTnsBlocked_shouldDropMail() {
        template.execute("INSERT INTO external_user_tns (status, email) VALUES ('blocked', '" + FROM_EMAIL + "')");
        testRule.registerConfig(BlockedUserFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.assertNoMailArrives();
    }

    @After
    public void tearDown() {
        template.execute("TRUNCATE TABLE userdata");
        template.execute("TRUNCATE TABLE external_user_tns");
    }

    private static MailBuilder defaultMail() {
        return aNewMail()
                .from(FROM_EMAIL)
                .to("seller@example.com")
                .adId("12345")
                .plainBody("A sample message");
    }
}
