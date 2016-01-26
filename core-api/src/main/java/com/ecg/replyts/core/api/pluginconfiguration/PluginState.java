package com.ecg.replyts.core.api.pluginconfiguration;

/**
 * Runtime status of a plugin
 *
 * @author mhuttar
 */
public enum PluginState {
    /**
     * Plugin is enabled and will be used
     */
    ENABLED,
    /**
     * Plugin is disabled, thus will be ignored in execution.
     */
    DISABLED,
    /**
     * Plugin is in evaluation mode (only applies to filters). It's output will be saved but does not have any impact on
     * the mail that is to be filtered.
     */
    EVALUATION;
}
