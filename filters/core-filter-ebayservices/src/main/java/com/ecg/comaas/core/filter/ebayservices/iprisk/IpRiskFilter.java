package com.ecg.comaas.core.filter.ebayservices.iprisk;

import com.ebay.marketplace.security.v1.services.IPBadLevel;
import com.ebay.marketplace.security.v1.services.IPRatingInfo;
import com.ecg.comaas.core.filter.ebayservices.IpAddressExtractor;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import de.mobile.ebay.service.IpRatingService;
import de.mobile.ebay.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class IpRiskFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(IpRiskFilter.class);

    private final IpRiskFilterConfigHolder ipRiskFilterConfigHolder;
    private final IpRatingService ipRatingService;

    public IpRiskFilter(IpRiskFilterConfigHolder ipRiskFilterConfigHolder, IpRatingService ipRatingService) {
        this.ipRiskFilterConfigHolder = ipRiskFilterConfigHolder;
        this.ipRatingService = ipRatingService;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Optional<String> optionalIpAddress = IpAddressExtractor.retrieveIpAddress(messageProcessingContext);
        LOG.trace("Determining IP Risk for {}", optionalIpAddress.orElse("NO IP FOUND"));
        if (optionalIpAddress.isPresent()) {
            String ipAddress = optionalIpAddress.get();
            try {
                IPRatingInfo ipRating = ipRatingService.getIpRating(ipAddress);
                if (ipRating == null || ipRating.getIpBadLevel() == null) {
                    return Collections.emptyList();
                }

                IPBadLevel ipBadLevel = ipRating.getIpBadLevel();
                int score = ipRiskFilterConfigHolder.getRating(ipBadLevel);
                LOG.trace("IP {} got rating {} -> Score {}", ipAddress, ipBadLevel, score);
                if (score != 0) {
                    return ImmutableList.of(new FilterFeedback(ipAddress, "IP is rated as: " + ipRating.getIpBadLevel(),
                            score, FilterResultState.OK));
                }
            } catch (ServiceException e) {
                LOG.warn("Error while accessing ebay services: {}", e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }
}
