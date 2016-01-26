package com.ecg.replyts.core.api.sanitychecks;

/**
 * Parsable outcome of a sanity check.
 */
public enum Status {
    /**
     * Sanity check reports normal functionality (green)
     */
    OK,
    /**
     * Sanity check reports slightly abnormal functionality (yellow) that the application can cope with - e.g. connection to remote service is available but latency is extremely high.
     */
    WARNING,
    /**
     * Sanity check reports a total failure that the application can not cope with any longer (e.g. connection to database gone)
     */
    CRITICAL
}