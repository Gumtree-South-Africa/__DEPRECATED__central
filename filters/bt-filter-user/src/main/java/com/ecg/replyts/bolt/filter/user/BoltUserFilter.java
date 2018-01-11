package com.ecg.replyts.bolt.filter.user;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;

public class BoltUserFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BoltUserFilter.class);

    private final BoltUserFilterConfig filterConfig;

    private final RestTemplate restTemplate;

    public BoltUserFilter(BoltUserFilterConfig filterConfig, RestTemplate restTemplate) {
        this.filterConfig = filterConfig;
        this.restTemplate = restTemplate;
    }

    private boolean isUserBlacklisted(String email){
        return filterConfig.getBlackList().contains(email);
    }

    private boolean isUsersBlocked(UserSnapshot[] userSnapshots) {
        if (userSnapshots == null) {
            return false;
        }

        UserSnapshot[] blockedUsers = Arrays.stream(userSnapshots)
          .filter(u -> u.getUserStateDetail().startsWith("BLOCKED"))
          .toArray(UserSnapshot[]::new);

        LOG.info("Users {} are blocked", StringUtils.arrayToCommaDelimitedString(blockedUsers));

        return blockedUsers.length != 0;
    }

    private boolean isBlockedByRules(String email) {
        return filterConfig.getBlackListEmailPattern().stream().filter(obj -> matchFound(obj, email)).findFirst().isPresent();
    }

    private boolean isBlockedUserName(String username) {
    	if (username == null) {
            return false;
        }

    	return filterConfig.getBlackListUserNamePattern().stream().filter(obj -> matchFound(obj, username)).findFirst().isPresent();
    }

    private boolean matchFound(Pattern pattern, String value) {
        try {
            if (pattern.matcher(value).find()) {
                LOG.info("User {} is blocked by the black list rules", value);

                return true;
            } else {
                return false;
            }
        } catch (RuntimeException e) {
            LOG.error("Skipping Regular Expression '{}'", pattern, e);

            return false;
        }
    }

    private boolean isUsersNew(UserSnapshot[] userSnapshots) {
        if (userSnapshots == null) {
            return false;
        }

        UserSnapshot[] newUsers = Arrays.stream(userSnapshots)
          .filter(u -> isNew(u.getCreationDate()))
          .toArray(UserSnapshot[]::new);

        LOG.info("Users {} are new", StringUtils.arrayToCommaDelimitedString(newUsers));

        return newUsers.length != 0;
    }

    private boolean isNew(Date userDate) {
        if (userDate == null) {
            LOG.warn("User date should not be null");

            return false;
        }

        return LocalDateTime.ofInstant(userDate.toInstant(), ZoneId.systemDefault())
          .plusHours(filterConfig.getNewUserHours())
          .isAfter(LocalDateTime.now());
    }

    private UserSnapshot[] getBoltUserSnapshot(String buyerEmail, String sellerEmail) {
        String url = getKernelApiUrl(buyerEmail, sellerEmail);

        LOG.trace("Kernel URL: {}", url);

        try {
            return restTemplate.getForObject(url, UserSnapshot[].class);
        } catch (RestClientException e) {
            LOG.error("Exception making Kernel API call to {}", url, e);

            return null;
        }
    }

    private String getKernelApiUrl(String buyerEmail, String sellerEmail) {
        return UriComponentsBuilder.fromHttpUrl(filterConfig.getKernelApiUrl())
          .queryParam("emails", buyerEmail)
          .queryParam("emails", sellerEmail)
          .build(false).encode().toUriString();
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        ImmutableList.Builder<FilterFeedback> feedback = ImmutableList.builder();

        Conversation conversation = context.getConversation();

        // Check if either buyer/seller is blacklisted
        if (isUserBlacklisted(conversation.getBuyerId())) {
            LOG.info("Buyer in blacklist: {}", conversation.getBuyerId());

            return feedback.add(new FilterFeedback("Bolt-User-Filter", "Buyer in Blacklist", filterConfig.getBlackListUserScore(), FilterResultState.DROPPED)).build();

        }
        if (isUserBlacklisted(conversation.getSellerId())) {
            LOG.info("Seller in blacklist: {}", conversation.getSellerId());

            return feedback.add(new FilterFeedback("Bolt-User-Filter", "Seller in Blacklist", filterConfig.getBlackListUserScore(), FilterResultState.DROPPED)).build();
        }

        if (isBlockedByRules(conversation.getBuyerId())) {
            LOG.info("Buyer in black listed rules: {}", conversation.getBuyerId());

            return feedback.add(new FilterFeedback("Bolt-User-Filter", "Buyer in black listed rules", filterConfig.getBlackListUserScore(), FilterResultState.DROPPED)).build();

        }
        if (isBlockedByRules(conversation.getSellerId())) {
            LOG.info("Seller in black listed rules: {}", conversation.getSellerId());

            return feedback.add(new FilterFeedback("Bolt-User-Filter", "Seller in black listed rules", filterConfig.getBlackListUserScore(), FilterResultState.DROPPED)).build();
        }

        // Call Kernel API to fetch userStatus
        UserSnapshot[] userSnapshots = getBoltUserSnapshot(conversation.getBuyerId(), conversation.getSellerId());

        // Check if buyer/seller is blocked on BOLT
        if (isUsersBlocked(userSnapshots)) {
            LOG.info("Seller in blacklist: {}", conversation.getSellerId());

            feedback.add(new FilterFeedback("Bolt-User-Filter", "User blocked on BOLT", filterConfig.getBlockUserScore(), FilterResultState.DROPPED));
        }

        // Check if buyer/seller is new on BOLT
        if (isUsersNew(userSnapshots)) {
            feedback.add(new FilterFeedback("Bolt-User-Filter", "User is NEW on BOLT", filterConfig.getNewUserScore(), FilterResultState.OK));
        }

        String buyerName = context.getConversation().getCustomValues().get("buyer-name");
        
        if (isBlockedUserName(buyerName)) {
            LOG.info("Buyer name {} in black listed rules", buyerName);

            return feedback.add(new FilterFeedback("Bolt-User-Filter", "Buyer Name in black listed rules", filterConfig.getBlackListUserScore(), FilterResultState.DROPPED)).build();
        }

        return feedback.build();
    }
}