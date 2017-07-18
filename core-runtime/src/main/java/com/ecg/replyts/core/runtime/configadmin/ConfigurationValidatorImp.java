package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigurationValidatorImp implements ConfigurationUpdateNotifier {
    @Autowired
    private List<ConfigurationAdmin<?>> configAdmins;

    @Autowired
    private ClusterRefreshPublisher publisher;

    @Override
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

    @Override
    public void confirmConfigurationUpdate() {
        publisher.publish();
    }
}