package com.gumtree.replyts2.plugins.reporting.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;


/**
 * Very simple JDBC implementation for message queueing and de-queueing.
 * Author: bpadhiar
 */
public class JdbcMessageQueueManager implements MessageQueueManager {

    private static final String INSERT_SQL =
            "insert into rts2_message_queue (message_id, category_id, timestamp) values (?, ?, ?)";

    private static final String DELETE_SQL = "delete from rts2_message_queue where message_id = ?";
    private JdbcTemplate jdbcTemplate;
    private static final Logger LOG = LoggerFactory.getLogger(JdbcMessageQueueManager.class);

    public JdbcMessageQueueManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void enQueueMessage(String messageId, Long categoryId, Date date) {
        try {
            jdbcTemplate.update(INSERT_SQL, messageId, categoryId, date);
        } catch (Exception e) {
            LOG.warn(String.format("Held message could not be inserted into db queue: %s", messageId), e);
        }

    }

    public boolean deQueueMessage(String messageId) {
        try {
            int rows = jdbcTemplate.update(DELETE_SQL, messageId);
            return rows > 0;
        } catch (Exception e) {
            LOG.warn(String.format("Queued message could not be removed from db queue: %s", messageId), e);
            return false;
        }

    }
}
