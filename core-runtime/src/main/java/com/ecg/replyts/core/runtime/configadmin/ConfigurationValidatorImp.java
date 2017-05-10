package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationUpdateNotifier;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ConfigurationValidatorImp implements ConfigurationUpdateNotifier {
    @Autowired
    private List<ConfigurationAdmin<?>> configAdmins;

    @Autowired
    private RefresherManager refreshers;

    @Autowired
    private ClusterRefreshPublisher publisher;

    @Override
    public boolean validateConfiguration(PluginConfiguration configuration) throws Exception {
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
    public void confirmConfigurationUpdate() throws Exception {
        refreshers.updateNow();
        publisher.publish();
    }
}