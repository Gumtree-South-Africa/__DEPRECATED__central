package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;

public class ConfigurationValidator {

    private List<ConfigurationAdmin<?>> configAdmins;

    public ConfigurationValidator(List<ConfigurationAdmin<?>> configAdmins) {
        this.configAdmins = configAdmins;
    }

    public boolean validateConfiguration(PluginConfiguration configuration) {
        for (ConfigurationAdmin<?> a : configAdmins) {
            if (a.handlesConfiguration(configuration.getId())) {
                // Test configuration
                a.createPluginInstance(configuration);
                return true;
            }
        }
        return false;
    }
}