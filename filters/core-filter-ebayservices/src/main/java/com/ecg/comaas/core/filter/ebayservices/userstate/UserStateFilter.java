package com.ecg.comaas.core.filter.ebayservices.userstate;

import com.ebay.marketplace.user.v1.services.UserEnum;
import com.ecg.comaas.core.filter.ebayservices.userstate.provider.UserStateProvider;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UserStateFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(UserStateFilter.class);

    private final UserStateFilterConfigHolder configHolder;
    private final UserStateProvider userStateProvider;

    public UserStateFilter(UserStateFilterConfigHolder configHolder, UserStateProvider userStateProvider) {
        this.configHolder = configHolder;
        this.userStateProvider = userStateProvider;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Optional<Mail> mail = messageProcessingContext.getMail();
        if (!mail.isPresent()) {
            return Collections.emptyList();
        }

        String sender = mail.get().getFrom();
        UserEnum userState = userStateProvider.getSenderState(sender);
        if (userState == null) {
            return Collections.emptyList();
        }

        int score = configHolder.getUserStateScore(userState);
        LOG.trace("User State for {} is {} --> Score: {}", sender, userState, score);
        if (score != 0) {
            return ImmutableList.of(
                    new FilterFeedback(sender, "User state is: " + userState, score, FilterResultState.OK));
        }
        return Collections.emptyList();
    }
}
