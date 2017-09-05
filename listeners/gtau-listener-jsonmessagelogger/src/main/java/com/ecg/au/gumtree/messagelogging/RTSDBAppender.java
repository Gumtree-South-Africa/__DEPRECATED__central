package com.ecg.au.gumtree.messagelogging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.db.DBAppenderBase;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;


/**
 * @author mdarapour
 */
public class RTSDBAppender extends DBAppenderBase<ILoggingEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(RTSDBAppender.class);

    private static final JsonNode EMPTY_NODE = new TextNode("-");

    protected static final String insertSQL;

    static {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO rts2_event_log (");
        sql.append("messageId, ");
        sql.append("conversationId, ");
        sql.append("messageDirection, ");
        sql.append("conversationState, ");
        sql.append("messageState, ");
        sql.append("adId, ");
        sql.append("sellerMail, ");
        sql.append("buyerMail, ");
        sql.append("numOfMessageInConversation, ");
        sql.append("logTimestamp, ");
        sql.append("conversationCreatedAt, ");
        sql.append("messageReceivedAt, ");
        sql.append("conversationLastModifiedDate, ");
        sql.append("custcategoryid, ");
        sql.append("custip, ");
        sql.append("custuseragent, ");
        sql.append("custreplychannel) ");
        sql.append(" VALUES (?, ?, ? ,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        insertSQL = sql.toString();
    }

    @Override
    protected Method getGeneratedKeysMethod() {
        return null;
    }

    @Override
    protected String getInsertSQL() {
        return insertSQL;
    }

    @Override
    protected void subAppend(ILoggingEvent event, Connection connection, PreparedStatement statement) throws Throwable {
        try {
            addEvent(statement, event);

            int updateCount = statement.executeUpdate();
            if (updateCount != 1) {
                log("WARN: Failed to insert json event");
                addWarn("Failed to insert json event");
            }
            LOG.trace("Successfully wrote event");
        } catch (Throwable ex) {
            log("ERROR: Could not log the event", ex);
            throw ex;
        }
    }

    @Override
    protected void secondarySubAppend(ILoggingEvent event, Connection connection, long id) throws Throwable {
        // No header is supported
    }

    void addEvent(PreparedStatement stmt, ILoggingEvent event) throws SQLException {
        JsonNode jevent = JsonObjects.parse(event.getMessage());

        stmt.setString(1, asText(jevent.get("messageId")));
        stmt.setString(2, asText(jevent.get("conversationId")));
        stmt.setString(3, asText(jevent.get("messageDirection")));
        stmt.setString(4, asText(jevent.get("conversationState")));
        stmt.setString(5, asText(jevent.get("messageState")));
        stmt.setString(6, asText(jevent.get("adId")));
        stmt.setString(7, asText(jevent.get("sellerMail")));
        stmt.setString(8, asText(jevent.get("buyerMail")));
        stmt.setString(9, asText(jevent.get("numOfMessageInConversation")));
        stmt.setString(10, asText(jevent.get("logTimestamp")));
        stmt.setString(11, asText(jevent.get("conversationCreatedAt")));
        stmt.setString(12, asText(jevent.get("messageReceivedAt")));
        stmt.setString(13, asText(jevent.get("conversationLastModifiedDate")));
        stmt.setString(14, asText(jevent.get("custcategoryid")));
        stmt.setString(15, asText(jevent.get("custip")));
        stmt.setString(16, asText(jevent.get("custuseragent")));
        stmt.setString(17, asText(jevent.get("custreplychannel")));
    }

    private void log(final String message) {
        log(message, null);
    }

    private void log(final String message, final Throwable ex) {
        System.err.println(message);
        if (ex!=null) {
            ex.printStackTrace();
        }

        LOG.error(message, ex);
    }

    private String asText(JsonNode node) {
        return Optional.ofNullable(node).orElse(EMPTY_NODE).asText();
    }
}
