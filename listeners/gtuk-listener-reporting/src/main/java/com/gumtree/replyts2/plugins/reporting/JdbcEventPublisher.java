package com.gumtree.replyts2.plugins.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

public class JdbcEventPublisher implements EventPublisher {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(JdbcEventPublisher.class);

    public static final String EVENT_LOG_TABLE = "rts2_event_log";

    private SimpleJdbcInsert insertEvent;

    /**
     * Constructor.
     *
     * @param dataSource the datasource to write to
     */
    public JdbcEventPublisher(DataSource dataSource) {
        this.insertEvent = new SimpleJdbcInsert(dataSource).withTableName(EVENT_LOG_TABLE);
        insertEvent.setGeneratedKeyName("id");
    }

    @Override
    public void publish(MessageProcessedEvent event) {
        try {
            Map<String, Object> parameters = new HashMap<>();
            addToParameters("message_id", event.getMessageId(), parameters);
            addToParameters("conversation_id", event.getConversationId(), parameters);
            addToParameters("message_direction", event.getMessageDirection().ordinal() + 1, parameters);
            addToParameters("conversation_state", event.getConversationState().ordinal() + 1, parameters);
            addToParameters("message_state", event.getMessageState().ordinal() + 1, parameters);
            addToParameters("filter_result_state", event.getFilterResultState().ordinal() + 1, parameters);
            addToParameters("human_result_state", event.getHumanResultState().ordinal() + 1, parameters);
            addToParameters("ad_id", event.getAdId(), parameters);
            addToParameters("seller_mail", event.getSellerMail(), parameters);
            addToParameters("buyer_mail", event.getBuyerMail(), parameters);
            addToParameters("num_of_message_in_conversation", event.getNumOfMessageInConversation(), parameters);
            addToParameters("timestamp", new Date(event.getTimestamp().getMillis()), parameters);
            addToParameters("conversation_created_at", new Date(event.getConversationCreatedAt().getMillis()), parameters);
            addToParameters("message_received_at", new Date(event.getMessageReceivedAt().getMillis()), parameters);
            addToParameters("conversation_last_modified_at", new Date(event.getConversationLastModifiedAt().getMillis()), parameters);
            addToParameters("message_last_modified_at", new Date(event.getMessageLastModifiedAt().getMillis()), parameters);
            addToParameters("category_id", event.getCustomCategoryId(), parameters);

            // Inet address
            PGobject inetObject = new PGobject();
            inetObject.setType("inet");
            inetObject.setValue(event.getCustomIp());
            addToParameters("buyer_ip", inetObject, parameters);

            // Processing Feedback JSON blob
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(mapper.writeValueAsString(event.filterResults));
            addToParameters("processing_feedback", jsonObject, parameters);

            insertEvent.execute(parameters);

        } catch (Exception ex) {
            LOG.error(String.format("Failed to write event to event log for message %s in conversation %s",
                    event.getMessageId(),
                    event.getConversationId()));
        }
    }

    private void addToParameters(String key, Object value, Map<String, Object> parameters) {
        parameters.put(key, value);
    }

}


