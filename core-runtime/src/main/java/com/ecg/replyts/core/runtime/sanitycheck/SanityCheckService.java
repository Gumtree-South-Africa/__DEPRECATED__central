package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(value = "cluster.jmx.enabled", havingValue = "true", matchIfMissing = true)
public class SanityCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(SanityCheckService.class);

    private final JmxPropagator jmxPropagator = new JmxPropagator("ReplyTS");

    @Autowired(required = false)
    private List<CheckProvider> providers = Collections.emptyList();

    @PostConstruct
    public void addChecksAndStart() {
        LOG.info("Registering Check Providers from {}", providers);

        for (CheckProvider p : providers) {
            jmxPropagator.addCheck(p.getChecks());
        }

        jmxPropagator.start();

        LOG.info("Sanity Check Service started");
    }

    @PreDestroy
    public void stop() {
        jmxPropagator.stop();

        LOG.info("Sanity Check Service shutdown");
    }
}