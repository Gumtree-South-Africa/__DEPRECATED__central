package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk;

import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.impl.Config;
import de.mobile.ebay.service.impl.IpRatingServiceImpl;
import de.mobile.ebay.service.OAuthTokenProvider;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * User: acharton
 * Date: 12/18/12
 */
@Component
class IpRiskFilterFactory implements FilterFactory {

    private final IpRatingService ipRatingService;

    @Autowired
    public IpRiskFilterFactory(HttpClient client, @Qualifier("esconfig-ipaddr") Config config, @Qualifier("ebayOAuthTokenProvider") OAuthTokenProvider tokenService) {
        this.ipRatingService = new IpRatingServiceImpl(client, config, tokenService);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new IpRiskFilter(s, new IpLevelConfigParser(jsonNode).parse(),ipRatingService, new IpAddressExtractor());
    }
}
