package com.ecg.comaas.gtau.filter.ebayservices.iprisk;

import com.ecg.comaas.gtau.filter.ebayservices.IpAddressExtractor;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.OAuthTokenProvider;
import de.mobile.ebay.service.impl.Config;
import de.mobile.ebay.service.impl.IpRatingServiceImpl;
import org.apache.http.client.HttpClient;

public class IpRiskFilterFactory implements FilterFactory {

    private static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk.IpRiskFilterFactory";

    private final IpRatingService ipRatingService;

    public IpRiskFilterFactory(HttpClient client, Config config, OAuthTokenProvider tokenService) {
        this.ipRatingService = new IpRatingServiceImpl(client, config, tokenService);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new IpRiskFilter(s, new IpLevelConfigParser(jsonNode).parse(), ipRatingService, new IpAddressExtractor());
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
