package com.ebay.ecg.bolt.domain.service.push;

import java.util.Hashtable;
import java.util.Map;

import com.ebay.ecg.bolt.domain.service.push.model.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ebay.ecg.bolt.api.server.push.model.PushProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Component
public class PushMessageServiceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PushMessageServiceConfig.class);
    
    private final Map<PushProvider, PushRequester> pushRequesters = new Hashtable<>();

    @Autowired
    public PushMessageServiceConfig(
      @Value("${gcm.host:}") String gcmHost,
      @Value("${gcm.apiKey:}") String gcmApiKey,
      @Value("${gcm.searchalerts.priority:}") String searchAlertsPriority,
      @Value("${mdns.host}") String mdnsHost,
      @Value("${mdns.authHeader}") String mdnsAuthHeader,
      @Value("${mdns.provider}") String mdnsProvider) {
        if (StringUtils.hasText(gcmHost)) {
            PushHostInfo pushHostInfo = new PushHostInfo(gcmHost, gcmApiKey, null);

            pushRequesters.put(PushProvider.gcm, new GCMRequester(pushHostInfo, searchAlertsPriority));

            LOG.info("PushProvider: {}{}", PushProvider.gcm.name(), pushHostInfo);

            pushRequesters.put(PushProvider.pwa, new PWARequester(pushHostInfo));
        }
        
        if (StringUtils.hasText(mdnsHost) && StringUtils.hasText(mdnsAuthHeader) && StringUtils.hasText(mdnsProvider)) {
            PushHostInfo pushHostInfo = new PushHostInfo(mdnsHost, mdnsAuthHeader, mdnsProvider);

            pushRequesters.put(PushProvider.mdns, new MDNSRequester(pushHostInfo));

            LOG.info("PushProvider: {}{}", PushProvider.mdns.name(), pushHostInfo);
        }
    }
    
    public PushRequester findPushRequester(PushProvider pushProvider) {
        if (pushProvider.name().equalsIgnoreCase("apns")) { // Fix for BOLT-20378
            pushProvider = PushProvider.mdns;
        }

        return pushRequesters.get(pushProvider);
    }
}