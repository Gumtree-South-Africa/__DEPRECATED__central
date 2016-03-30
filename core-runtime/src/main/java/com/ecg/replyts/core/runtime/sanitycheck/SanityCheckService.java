package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * Class that manages the ecg sanity check framework. Receives a list of {@link CheckProvider}s and registers all their
 * checks to the {@link JmxPropagator}. JMX beans can be started/stopped via {@link #start()} and {@link #stop()}. <br/>
 * This framework is based on the work of the mobile sanity check frameworks that supports sanity checking via JMX.
 *
 * @author mhuttar
 */
public class SanityCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(SanityCheckService.class);

    private final JmxPropagator jmxPropagator = new JmxPropagator("ReplyTS");

    private boolean isJmxEnabled;

    @Autowired
    public SanityCheckService(@Value("${cluster.jmx.enabled:true}") boolean isJmxEnabled, List<CheckProvider> providers) {
        this.isJmxEnabled = isJmxEnabled;

        if (isJmxEnabled) {
            LOG.info("Registering Check Providers from {}", providers);

            for (CheckProvider p : providers) {
                jmxPropagator.addCheck(p.getChecks());
            }
        }
    }

    @PostConstruct
    void start() {
        if (isJmxEnabled) {
            jmxPropagator.start();

            LOG.info("Sanity Check Service started");
        }
    }

    @PreDestroy
    void stop() {
        if (isJmxEnabled) {
            jmxPropagator.stop();

            LOG.info("Sanity Check Service shutdown");
        }
    }
}
