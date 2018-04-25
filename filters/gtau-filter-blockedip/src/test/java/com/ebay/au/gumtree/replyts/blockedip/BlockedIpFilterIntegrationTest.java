package com.ebay.au.gumtree.replyts.blockedip;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Properties;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;

public class BlockedIpFilterIntegrationTest {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/comaas";
    private static final String DB_USER = "mysql_guest";
    private static final String DB_PASS = "mysql_guest";
    private static final String DEFAULT_IP = "127.0.0.1";

    private final Properties testProperties = new Properties() {{
        put("replyts2-blockedipfilter-plugin.dataSource.url", DB_URL);
        put("replyts2-blockedipfilter-plugin.dataSource.username", DB_USER);
        put("replyts2-blockedipfilter-plugin.dataSource.password", DB_PASS);
        put("replyts2-blockedipfilter-plugin.dataSource.pool.maxPoolSize", 10);
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
    public void whenIpRangeEmpty_shouldSendMail() {
        testRule.registerConfig(BlockedIpFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.waitForMail();
    }

    @Test
    public void whenIpBlocked_shouldDropMail() {
        Timestamp fiveMinsFromNow = new Timestamp(System.currentTimeMillis() + 300_000);
        template.update("INSERT INTO ip_ranges (expiration_date, begin_ip, end_ip) VALUES (?, ?, ?)", fiveMinsFromNow, DEFAULT_IP, DEFAULT_IP);
        testRule.registerConfig(BlockedIpFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.assertNoMailArrives();
    }

    @Test
    public void whenTwoExpiryDates_andOneIsInFuture_shouldDropMail() {
        Timestamp fiveMinsAgo = new Timestamp(System.currentTimeMillis() - 300_000);
        Timestamp fiveMinsFromNow = new Timestamp(System.currentTimeMillis() + 300_000);
        template.update("INSERT INTO ip_ranges (expiration_date, begin_ip, end_ip) VALUES (?, ?, ?)", fiveMinsAgo, DEFAULT_IP, DEFAULT_IP);
        template.update("INSERT INTO ip_ranges (expiration_date, begin_ip, end_ip) VALUES (?, ?, ?)", fiveMinsFromNow, DEFAULT_IP, DEFAULT_IP);
        testRule.registerConfig(BlockedIpFilterFactory.IDENTIFIER, null);

        testRule.deliver(defaultMail());
        testRule.assertNoMailArrives();
    }

    @After
    public void tearDown() {
        template.execute("TRUNCATE TABLE ip_ranges");
    }

    private static MailBuilder defaultMail() {
        return aNewMail()
                .from("buyer@example.com")
                .to("seller@example.com")
                .adId("12345")
                .plainBody("A sample message")
                .customHeader("Ip", DEFAULT_IP);
    }
}
