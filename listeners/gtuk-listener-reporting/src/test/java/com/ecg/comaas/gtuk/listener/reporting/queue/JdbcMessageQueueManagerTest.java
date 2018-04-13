package com.ecg.comaas.gtuk.listener.reporting.queue;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Author: bpadhiar
 */
public class JdbcMessageQueueManagerTest {

    private JdbcTemplate jdbcTemplate;
    private Date testDate;

    @Before
    public void setup() {
        jdbcTemplate = mock(JdbcTemplate.class);
        testDate = new DateTime(2015, 5, 30, 1, 0, 0).toDate();
    }

    @Test
    public void testEnqueueInsert() {
        JdbcMessageQueueManager jdbcMessageQueueManager = new JdbcMessageQueueManager(jdbcTemplate);
        jdbcMessageQueueManager.enQueueMessage("messageId", 1234L, testDate);
        verify(jdbcTemplate).update(
                eq("insert into rts2_message_queue (message_id, category_id, timestamp) values (?, ?, ?)"),
                eq("messageId"), eq(1234L),
                eq(testDate));
    }

    @Test
    public void testDeQueueDeleteRemoves1Row() {
        JdbcMessageQueueManager jdbcMessageQueueManager = new JdbcMessageQueueManager(jdbcTemplate);
        when(jdbcTemplate.update("delete from rts2_message_queue where message_id = ?", "messageId")).thenReturn(1);

        boolean success = jdbcMessageQueueManager.deQueueMessage("messageId");

        verify(jdbcTemplate).update(
                eq("delete from rts2_message_queue where message_id = ?"),
                eq("messageId"));
        assertTrue(success);
    }

    @Test
    public void testDeQueueDeleteRemovesNoRow() {
        JdbcMessageQueueManager jdbcMessageQueueManager = new JdbcMessageQueueManager(jdbcTemplate);
        when(jdbcTemplate.update("delete from rts2_message_queue where message_id = ?", "messageId")).thenReturn(0);

        boolean success = jdbcMessageQueueManager.deQueueMessage("messageId");

        verify(jdbcTemplate).update(
                eq("delete from rts2_message_queue where message_id = ?"),
                eq("messageId"));
        assertFalse(success);
    }

    @Test
    public void testDeQueueThrowsException() {
        JdbcMessageQueueManager jdbcMessageQueueManager = new JdbcMessageQueueManager(jdbcTemplate);
        when(jdbcTemplate.update("delete from rts2_message_queue where message_id = ?", "messageId"))
                .thenThrow(new RuntimeException("test failure"));

        boolean success = jdbcMessageQueueManager.deQueueMessage("messageId");

        verify(jdbcTemplate).update(
                eq("delete from rts2_message_queue where message_id = ?"),
                eq("messageId"));
        assertFalse(success);
    }

}