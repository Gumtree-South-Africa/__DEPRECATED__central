package com.ecg.comaas.kjca.listener.userbehaviour.reporter.sink;


import com.ecg.comaas.kjca.listener.userbehaviour.model.ResponsivenessRecord;

public interface ResponsivenessSink {
    /**
     * @param writerId Unique identifier of the caller, such as thread id
     * @param record The record to write out
     */
    void storeRecord(String writerId, ResponsivenessRecord record);
}
