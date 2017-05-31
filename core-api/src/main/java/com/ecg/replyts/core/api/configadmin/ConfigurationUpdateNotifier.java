package com.ecg.replyts.core.api.configadmin;


/**
 * Describes a validator that is able to forecast whether a specific configuration can be handled by ReplyTS or not.
 *
 * @author mhuttar
 */
public interface ConfigurationUpdateNotifier {
    /**
     * checks it there is a {@link com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory} that can handle this configuration and also dry runs the
     * configuration (create a new plugin instance of it to see if this configuration is parseable)
     *
     * @param configuration cfg to test
     * @return <code>true</code> if configuration is guaranteed to work. <code>false</code> if there is noone being able
     * to handle that configuration.
     */
    boolean validateConfiguration(PluginConfiguration configuration);

    void confirmConfigurationUpdate();
}
