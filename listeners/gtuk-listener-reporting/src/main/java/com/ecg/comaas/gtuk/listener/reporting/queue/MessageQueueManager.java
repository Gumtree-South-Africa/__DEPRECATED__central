package com.ecg.comaas.gtuk.listener.reporting.queue;

import java.util.Date;

/**
 * Responsible for adding messages to the 'screening' queue and removing message from the queue.
 * The main purpose of this to allow CS teams to be able to monitor message queues by category in real time.
 *
 */
public interface MessageQueueManager {

    /**
     * Add a message to the queue.
     * @param messageId - The message Id
     * @param categoryId - The category Id related to this message
     * @param date - The time of insertion
     */
    void enQueueMessage(String messageId, Long categoryId, Date date);

    /**
     * Removes a message from the queue.
     * @param messageId - The message Id. The message to remove.
     * @return true if one message was successfully removed. False otherwise.
     *
     */
    boolean deQueueMessage(String messageId);

}
