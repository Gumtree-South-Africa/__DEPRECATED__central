package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    private List<PluginConfiguration> configsFromRepo = new ArrayList<PluginConfiguration>();

    private List<PluginInstanceReference<Object>> runningPlugins = new ArrayList<PluginInstanceReference<Object>>();

    @Before
    public void setup() {
        when(repo.getConfigurations()).thenReturn(configsFromRepo);
        when(admin.getRunningServices()).thenReturn(runningPlugins);
        when(admin.handlesConfiguration(any(ConfigurationId.class))).thenReturn(true);
    }

    @Test
    public void launchesNewConfigurations() throws Exception {
        PluginConfiguration pi = pi(String.class, "instance1", 1l);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(1)).putConfiguration(pi);
    }

    @Test
    public void updatesExistingConfigurationWhenNewer() throws Exception {
        PluginConfiguration pi = pi(String.class, "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<Object>(pi, new Object()));
        PluginConfiguration pi2 = pi(String.class, "instance1", 4l);
        configsFromRepo.add(pi2);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(1)).putConfiguration(pi2);

    }

    @Test
    public void doesNotUpdateExistingConfigurationWhenSameVersion() throws Exception {
        PluginConfiguration pi = pi(String.class, "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<Object>(pi, new Object()));
        PluginConfiguration pi2 = pi(String.class, "instance1", 1l);
        configsFromRepo.add(pi2);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(0)).putConfiguration(pi2);
    }

    @Test
    public void addsSecondConfiguration() {
        PluginConfiguration pi = pi(String.class, "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<Object>(pi, new Object()));
        PluginConfiguration pi2 = pi(String.class, "instance2", 4l);
        configsFromRepo.add(pi2);
        configsFromRepo.add(pi);

        r.updateConfigurations();

        verify(admin, times(0)).deleteConfiguration(pi.getId());
        verify(admin, times(1)).putConfiguration(pi2);
    }

    @Test
    public void deletesOldConfiguration() {
        PluginConfiguration pi = pi(String.class, "instance1", 1l);
        runningPlugins.add(new PluginInstanceReference<Object>(pi, new Object()));

        r.updateConfigurations();

        verify(admin, times(1)).deleteConfiguration(pi.getId());
    }

    private PluginConfiguration pi(Class serviceType, String instanceId, long version) {
        return new PluginConfiguration(new ConfigurationId(serviceType, instanceId), 1l, PluginState.ENABLED, version, JsonObjects.builder().build());
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
