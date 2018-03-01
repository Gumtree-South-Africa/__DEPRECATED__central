package com.ecg.messagecenter.diff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "webapi.sync.au.enabled", havingValue = "true")
public class WebApiSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(WebApiSyncService.class);

    @PostConstruct
    public void init() {
        LOG.info("WebApiSyncService for AU initialized!!!");
    }
}
