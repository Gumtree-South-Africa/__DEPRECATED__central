package com.ecg.replyts.core.runtime.sanitycheck.adapter;


public interface CheckAdapterMBean {

    /**
     * @return The status (CRITICAL|WARNING|OK).
     */
    String getStatus();

    /**
     * @return A message to the check.
     */
    String getMessage();

    void execute();

}
