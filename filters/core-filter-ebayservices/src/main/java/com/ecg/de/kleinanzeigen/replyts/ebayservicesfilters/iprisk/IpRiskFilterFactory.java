package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk;

import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.OAuthTokenProvider;
import de.mobile.ebay.service.impl.IpRatingServiceImpl;
import de.mobile.ebay.service.impl.ServiceConfigBean;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * User: acharton
 * Date: 12/18/12
 */
@Component
class IpRiskFilterFactory implements FilterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(IpRiskFilterFactory.class);

    private final IpRatingService ipRatingService;

    @Autowired
    public IpRiskFilterFactory(HttpClient client, @Qualifier("esconfig-ipaddr") ServiceConfigBean config, @Qualifier("ebayOAuthTokenProvider") OAuthTokenProvider tokenService) {
        LOG.info("Configured proxy for IpRiskFilter: '{}'", config.proxyUrl());
        this.ipRatingService = new IpRatingServiceImpl(client, config, tokenService);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new IpRiskFilter(s, new IpLevelConfigParser(jsonNode).parse(),ipRatingService, new IpAddressExtractor());
    }
}
