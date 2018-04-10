package com.ecg.comaas.ebayk.listener.dailyreport.hadoop;

/**
 * Created by johndavis on 29/11/16.
 */
public interface HadoopEventEmitter {
    void insert(HadoopLogEntry entry);
}
