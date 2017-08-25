package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ebay.marketplace.user.v1.services.MemberBadgeDataType;
import com.ebay.marketplace.user.v1.services.UserEnum;
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

    private final Map<String, Integer> config;
    private final UserService userService;

    public UserStateFilter(Map<String, Integer> config, UserService userService) {
        this.config = config;
        this.userService = userService;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String sender = messageProcessingContext.getMail().getFrom();
        try {
            MemberBadgeDataType badgeData = userService.getMemberBadgeData(sender);

            if (badgeData != null) {
                UserEnum userState = badgeData.getUserState();
                // User state may be null, when JAXB encounters an unknown value for the field
                // e.g. UNCONFIRMED, which is not included in the library, yet
                // That is why we have to check it here.
                // Check also if configuration contains the key.
                if (userState == null || !config.containsKey(userState.name())) {
                    return Collections.emptyList();
                }
                int score = config.get(userState.name());
                LOG.trace("User State for {} is {} --> Score: {}", sender, userState, score);
                if (score != 0) {
                    return ImmutableList.of(
                            new FilterFeedback(
                                    sender,
                                    "User state is: " + userState,
                                    score,
                                    FilterResultState.OK));
                }

            }
        } catch (ServiceException e) {
            String message = e.getMessage();
            if (message == null) {
                LOG.error("Error while calling eBay Service. No Details given", e);
            } else if (!message.contains("User Not Found")) {
                LOG.warn("Error while calling ebay service, details: " + message);
            }
        }
        return Collections.emptyList();
    }
}
