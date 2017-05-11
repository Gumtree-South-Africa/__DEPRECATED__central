package com.ecg.replyts.core.runtime.configadmin;

public interface ConfigurationRefreshEventListener {
    /**
     * Return true to enable the unregister hook
     *
     * @param clazz the ? extends BasePluginFactory classname
     * @return true is unregister should be called
     */
    boolean notify(Class<?> clazz);

    /**
     * Put any unregister code in this method
     * @param instanceName the name of the plugin instance
     */
    void unregister(String instanceName);
}