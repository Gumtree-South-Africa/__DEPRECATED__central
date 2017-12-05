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
    private static final Logger LOG = LoggerFactory.getLogger(JdbcEventPublisher.class);

    private static ObjectMapper mapper = new ObjectMapper();

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
            parameters.put("message_id", event.getMessageId());
            parameters.put("conversation_id", event.getConversationId());
            parameters.put("message_direction", event.getMessageDirection().ordinal() + 1);
            parameters.put("conversation_state", event.getConversationState().ordinal() + 1);
            parameters.put("message_state", event.getMessageState().ordinal() + 1);
            parameters.put("filter_result_state", event.getFilterResultState().ordinal() + 1);
            parameters.put("human_result_state", event.getHumanResultState().ordinal() + 1);
            parameters.put("ad_id", event.getAdId());
            parameters.put("seller_mail", event.getSellerMail());
            parameters.put("buyer_mail", event.getBuyerMail());
            parameters.put("num_of_message_in_conversation", event.getNumOfMessageInConversation());
            parameters.put("timestamp", new Date(event.getTimestamp().getMillis()));
            parameters.put("conversation_created_at", new Date(event.getConversationCreatedAt().getMillis()));
            parameters.put("message_received_at", new Date(event.getMessageReceivedAt().getMillis()));
            parameters.put("conversation_last_modified_at", new Date(event.getConversationLastModifiedAt().getMillis()));
            parameters.put("message_last_modified_at", new Date(event.getMessageLastModifiedAt().getMillis()));
            parameters.put("category_id", event.getCustomCategoryId());

            // Inet address
            PGobject inetObject = new PGobject();
            inetObject.setType("inet");
            inetObject.setValue(event.getCustomIp());
            parameters.put("buyer_ip", inetObject);

            // Processing Feedback JSON blob
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(mapper.writeValueAsString(event.filterResults));
            parameters.put("processing_feedback", jsonObject);

            insertEvent.execute(parameters);

        } catch (Exception ex) {
            LOG.error("Failed to write event to event log for message {} in conversation {}",
                    event.getMessageId(),
                    event.getConversationId(),
                    ex);
        }
    }
}
