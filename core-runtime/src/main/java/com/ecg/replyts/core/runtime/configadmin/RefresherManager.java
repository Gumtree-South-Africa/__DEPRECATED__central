package com.ecg.replyts.core.runtime.configadmin;

import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

// registered via context.
// @Component
class RefresherManager {

    private static final Logger LOG = LoggerFactory.getLogger(RefresherManager.class);

    @Autowired
    private List<ConfigurationAdmin<Object>> admins;

    @Autowired
    private ConfigurationRepository configRepo;

    @Autowired
    private ClusterRefreshSubscriber subscriber;

    private List<Refresher> refreshers = new ArrayList<Refresher>();

    @PostConstruct
    public void startRefreshing() {
        LOG.info("Starting to refresh configurations");
        for (ConfigurationAdmin<Object> a : admins) {
            Refresher r = new Refresher(configRepo, a); // .start();
            // ensure all configurations are loaded initially before the main mail processing threads kick in. 
            r.updateConfigurations();
            r.start();
            refreshers.add(r);
            subscriber.attach(r);
        }
    }


    public void updateNow() {
        for (Refresher r : refreshers) {
            r.updateConfigurations();
        }
    }
}
