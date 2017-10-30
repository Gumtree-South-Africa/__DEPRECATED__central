package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk;

import com.ebay.marketplace.security.v1.services.IPRatingInfo;
import com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.IpAddressExtractor;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: acharton
 * Date: 12/18/12
 * Time: 12:18 PM
 * Tyo change this template use File | Settings | File Templates.
 */
class IpRiskFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(IpRiskFilter.class);

    private String filtername;
    private IpRatingService ipRatingService;
    private IpAddressExtractor ipAddressExtractor;
    private Map<String, Integer> ipLevelMap;

    public IpRiskFilter(String filtername, Map<String, Integer> ipLevelMap , IpRatingService ipRatingService, IpAddressExtractor ipAddressExtractor) {
        this.filtername = filtername;
        this.ipLevelMap = ipLevelMap;
        this.ipRatingService = ipRatingService;
        this.ipAddressExtractor = ipAddressExtractor;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Optional<String> ipAddr = ipAddressExtractor.retrieveIpAddress(messageProcessingContext);
        LOG.debug("Determinating IP Risk for {}", ipAddr.orElse("NO IP FOUND"));
        if(ipAddr.isPresent()) {
            try {
                IPRatingInfo ipRating = ipRatingService.getIpRating(ipAddr.get());
                if(ipRating != null && ipRating.getIpBadLevel() != null) {
                    int score = ipLevelMap.get(ipRating.getIpBadLevel().name());
                    LOG.trace("IP {} got rating {} -> Score {}", ipAddr.get(), ipRating.getIpBadLevel().name(), score);
                    if(score != 0) {
                        return ImmutableList.<FilterFeedback>of(
                                new FilterFeedback(
                                        ipAddr.get(),
                                        "IP is rated as: " + ipRating.getIpBadLevel().name(),
                                        score,
                                        FilterResultState.OK));
                    }
                }
            } catch (ServiceException e) {
                LOG.warn("Error while accessing ebay services ({}): {}", stopwatch, e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }
}
