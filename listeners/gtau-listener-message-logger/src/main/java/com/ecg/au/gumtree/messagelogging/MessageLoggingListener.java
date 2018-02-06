package com.ecg.au.gumtree.messagelogging;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.mchange.v2.c3p0.DriverManagerDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;

@ComaasPlugin
@ConditionalOnProperty(value = "au.messagelogger.enabled", havingValue = "true", matchIfMissing = true)
public class MessageLoggingListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessageLoggingListener.class);

    private static final String INSERT_STATEMENT = "INSERT INTO rts2_event_log (messageId, conversationId, " +
      "messageDirection, conversationState, messageState, adId, sellerMail, buyerMail, numOfMessageInConversation, " +
      "logTimestamp, conversationCreatedAt, messageReceivedAt, conversationLastModifiedDate, custcategoryid, " +
      "custip, custuseragent, custreplychannel) VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private JdbcTemplate template;

    @Autowired
    private MessageLoggingListener(@Value("${au.messagelogger.driver}") String driver, @Value("${au.messagelogger.url}") String url, @Value("${au.messagelogger.username}") String username, @Value("${au.messagelogger.password}") String password) {
        DriverManagerDataSource source = new DriverManagerDataSource();

        source.setDriverClass(driver);
        source.setDriverClass(url);
        source.setDriverClass(username);
        source.setDriverClass(password);

        template = new JdbcTemplate(source);
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try {
            template.update(INSERT_STATEMENT, EventCreator.toValues(conversation, message).values());
        } catch (RuntimeException e) {
            LOG.error("Message logging failed", e);
        }
    }
}