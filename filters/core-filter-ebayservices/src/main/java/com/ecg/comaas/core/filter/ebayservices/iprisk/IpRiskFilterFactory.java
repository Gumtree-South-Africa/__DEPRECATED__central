package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.OAuthTokenProvider;
import de.mobile.ebay.service.impl.IpRatingServiceImpl;
import de.mobile.ebay.service.impl.ServiceConfigBean;
import org.apache.http.impl.client.CloseableHttpClient;

public class IpRiskFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk.IpRiskFilterFactory";

    private final IpRatingService ipRatingService;

    public IpRiskFilterFactory(CloseableHttpClient httpClient, ServiceConfigBean config, OAuthTokenProvider tokenService) {
        this.ipRatingService = new IpRatingServiceImpl(httpClient, config, tokenService);
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new IpRiskFilter(new IpRiskFilterConfigHolder(jsonNode), ipRatingService);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
