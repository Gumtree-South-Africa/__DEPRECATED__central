package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ebay.marketplace.user.v1.services.MemberBadgeDataType;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserService;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: acharton
 * Date: 12/18/12
 */
class UserStateFilter implements Filter {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(UserStateFilter.class);

    private final String filterInstance;
    private final Map<String, Integer> config;

    private UserService userService;

    public UserStateFilter(String filterInstance, Map<String, Integer> config, UserService userService) {
        this.filterInstance = filterInstance;
        this.config = config;
        this.userService = userService;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String sender = messageProcessingContext.getMail().get().getFrom();
        try {
            MemberBadgeDataType badgeData = userService.getMemberBadgeData(sender);

            if(badgeData != null) {
                int score = config.get(badgeData.getUserState().name());
                LOG.trace("User State for {} is {} --> Score: {}", sender, badgeData.getUserState().name(), score);
                if(score != 0) {
                    return ImmutableList.<FilterFeedback>of(
                            new FilterFeedback(
                                    sender,
                                    "User state is: " + badgeData.getUserState().name(),
                                    score,
                                    FilterResultState.OK));
                }

            }
        } catch (ServiceException e) {
            String message = e.getMessage();
            if(message == null) {
                LOG.error("Error while calling eBay Service. No Details given", e);
            } else if(!message.contains("User Not Found")) {
                LOG.warn("Error while calling ebay service, details: " + message);
            }
        }
        return Collections.emptyList();
    }
}
