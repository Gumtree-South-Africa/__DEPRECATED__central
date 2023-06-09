package com.ecg.comaas.gtau.listener.messagelogger;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

@ComaasPlugin
@Component
@Profile(TENANT_GTAU)
@ConditionalOnProperty(value = "au.messagelogger.enabled", havingValue = "true", matchIfMissing = true)
public class MessageLoggingListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageLoggingListener.class);
    private static final String INSERT_STATEMENT = "INSERT INTO rts2_event_log (messageId, conversationId, " +
            "messageDirection, conversationState, messageState, adId, sellerMail, buyerMail, numOfMessageInConversation, " +
            "logTimestamp, conversationCreatedAt, messageReceivedAt, conversationLastModifiedDate, custcategoryid, " +
            "custip, custuseragent, custreplychannel) VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private JdbcTemplate template;

    /**
     * MySQL tuning based on #{url https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration}
     */
    @Autowired
    public MessageLoggingListener(
            @Value("${au.messagelogger.url}") String url,
            @Value("${au.messagelogger.username}") String username,
            @Value("${au.messagelogger.password}") String password,
            @Value("${au.messagelogger.prepStmtCacheSize:250}") String cacheSize,
            @Value("${au.messagelogger.prepStmtCacheSqlLimit:2048}") String cacheLimit,
            @Value("${au.messagelogger.cachePrepStmts:true}") String cachePreparedStatements,
            @Value("${au.messagelogger.useServerPrepStmts:true}") String prepareOnServer
    ) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.addDataSourceProperty("prepStmtCacheSize", cacheSize);
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", cacheLimit);
        dataSource.addDataSourceProperty("cachePrepStmts", cachePreparedStatements);
        dataSource.addDataSourceProperty("useServerPrepStmts", prepareOnServer);

        template = new JdbcTemplate(dataSource);
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            if (EnumSet.of(MessageState.ORPHANED, MessageState.IGNORED).contains(message.getState())) {
                return;
            }
            Collection<String> values = EventCreator.toValues(conversation, message).values();

            template.update(INSERT_STATEMENT, values.toArray(new String[values.size()]));
        } catch (RuntimeException e) {
            reportExternalServiceFailure("mysql_store_event_log");
            LOG.error("Message logging failed", e);
        }
    }

    void setTemplate(JdbcTemplate template) {
        this.template = template;
    }
}
