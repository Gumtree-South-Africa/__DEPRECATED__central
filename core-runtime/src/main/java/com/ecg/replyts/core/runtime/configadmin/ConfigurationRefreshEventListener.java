package com.ecg.replyts.core.runtime.configadmin;

public interface ConfigurationRefreshEventListener {
    /**
     * Return true to enable the unregister hook
     *
     * @param String the plugin factory identifier
     * @return true is unregister should be called
     */
    boolean isApplicable(String identifier);

    /**
     * Put any unregister code in this method
     * @param instanceName the name of the plugin instance
     */
    void unregister(String instanceName);
}