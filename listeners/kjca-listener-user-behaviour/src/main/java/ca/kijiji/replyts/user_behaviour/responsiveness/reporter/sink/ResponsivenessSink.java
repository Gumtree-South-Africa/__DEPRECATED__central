package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;

public interface ResponsivenessSink {
    /**
     * @param writerId Unique identifier of the caller, such as thread id
     * @param record The record to write out
     */
    void storeRecord(String writerId, ResponsivenessRecord record);
}
