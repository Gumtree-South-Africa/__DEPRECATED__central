package com.ecg.comaas.gtau.listener.messagelogger;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageLoggingListenerIntegrationTest {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/comaas";
    private static final String DB_USER = "mysql_guest";
    private static final String DB_PASS = "mysql_guest";
    private static final String SELECT_EVENT_LOG = "SELECT * FROM rts2_event_log";
    private static final String TRUNCATE_EVENT_LOG = "TRUNCATE TABLE rts2_event_log";
    private static final String BUYER_EMAIL = "buyer@example.com";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String AD_ID = "12345";

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties());

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_GTAU);
        properties.put("replyts.tenant", TENANT_GTAU);
        properties.put("au.messagelogger.url", DB_URL);
        properties.put("au.messagelogger.username", DB_USER);
        properties.put("au.messagelogger.password", DB_PASS);
        return properties;
    }

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
    public void whenMessageProcessed_shouldStoreDataToMySql() {
        testRule.deliver(
                aNewMail()
                        .from(BUYER_EMAIL)
                        .to(SELLER_EMAIL)
                        .adId(AD_ID)
                        .plainBody("A sample message")
        );
        testRule.waitForMail();

        List<MySqlTestProjection> actual = template.query(SELECT_EVENT_LOG, (resultSet, i) -> new MySqlTestProjection(
                resultSet.getString("buyerMail"), resultSet.getString("sellerMail"), resultSet.getString("adId")));

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualToComparingFieldByField(new MySqlTestProjection(BUYER_EMAIL, SELLER_EMAIL, AD_ID));
    }

    @After
    public void tearDown() {
        template.execute(TRUNCATE_EVENT_LOG);
    }
}
