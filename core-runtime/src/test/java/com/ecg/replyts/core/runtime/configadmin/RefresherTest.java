package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RefresherTest.TestContext.class)
public class RefresherTest {
    @Autowired
    private ConfigurationRepository repo;

    @Autowired
    private ConfigurationAdmin<Object> admin;

    @Autowired
    private Refresher r;

    private List<PluginConfiguration> configsFromRepo = new ArrayList<>();

    private List<PluginInstanceReference<Object>> runningPlugins = new ArrayList<>();

    @Before
    public void setup() {
        when(repo.getConfigurations()).thenReturn(configsFromRepo);
        when(admin.getRunningServices()).thenReturn(runningPlugins);
        when(admin.handlesConfiguration(any(ConfigurationId.class))).thenReturn(true);
    }

    @Test
    public void launchesNewConfigurations() throws Exception {
        PluginConfiguration pi = pluginConfiguration("identifier", "instance1", 1l);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(1)).putConfiguration(pi);
    }

    @Test
    public void updatesExistingConfigurationWhenNewer() throws Exception {
        PluginConfiguration pi = pluginConfiguration("identifier", "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<>(pi, new Object()));
        PluginConfiguration pi2 = pluginConfiguration("identifier", "instance1", 4l);
        configsFromRepo.add(pi2);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(1)).putConfiguration(pi2);

    }

    @Test
    public void doesNotUpdateExistingConfigurationWhenSameVersion() throws Exception {
        PluginConfiguration pi = pluginConfiguration("identifier", "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<>(pi, new Object()));
        PluginConfiguration pi2 = pluginConfiguration("identifier", "instance1", 1l);
        configsFromRepo.add(pi2);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(0)).putConfiguration(pi2);
    }

    @Test
    public void addsSecondConfiguration() {
        PluginConfiguration pi = pluginConfiguration("identifier", "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<>(pi, new Object()));
        PluginConfiguration pi2 = pluginConfiguration("identifier", "instance2", 4l);
        configsFromRepo.add(pi2);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(1)).putConfiguration(pi2);
    }

    @Test
    public void deletesOldConfiguration() {
        PluginConfiguration pi = pluginConfiguration("identifier", "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<>(pi, new Object()));

        r.updateConfigurations();

        verify(admin, times(1)).deleteConfiguration(pi.getId());
    }

    private PluginConfiguration pluginConfiguration(String identifier, String instanceId, long version) {
        return PluginConfiguration.createGeneratingUuid(new ConfigurationId(identifier, instanceId), 1l, PluginState.ENABLED, version, JsonObjects.newJsonObject());
    }

    @Configuration
    static class TestContext {
        @MockBean
        private ConfigurationRepository configurationRepository;

        @MockBean
        private ConfigurationAdmin<Object> configurationAdmin;

        @Bean
        public Refresher refresher() {
            return new Refresher(configurationAdmin);
        }
    }
}
