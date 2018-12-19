package com.ecg.comaas.core.filter.activable;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ecg.comaas.core.filter.activable.MailHeader.CATEGORY_PATH;
import static com.ecg.comaas.core.filter.activable.MailHeader.USER_TYPE;

public abstract class ActivableFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ActivableFilter.class);

    private final Activation activation;

    protected ActivableFilter(Activation activation) {
        this.activation = activation;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {

        Map<String, String> headers = context.getMessage().getCaseInsensitiveHeaders();
        Set<Integer> categories = getCategories(headers);
        String userType = headers.get(USER_TYPE.getHeaderName());

        if (skipFor(categories) || !runFor(categories, userType)) {
            LOG.trace("Skip filter {} for categories {} and userType {}", this.getClass().getSimpleName(), categories, userType);
            return Collections.emptyList();
        }

        return doFilter(context);
    }

    private Set<Integer> getCategories(Map<String, String> headers) {
        String categoryPath = headers.get(CATEGORY_PATH.getHeaderName());
        if (categoryPath == null) {
            return new HashSet<>();
        }

        return Stream.of(categoryPath.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toSet());
    }

    private boolean skipFor(Set<Integer> categories) {
        return Sets.intersection(categories, activation.getExceptForCategories()).size() > 0;
    }

    private boolean runFor(Set<Integer> categories, String userType) {
        return (activation.getRunForCategories().isEmpty() || Sets.intersection(categories, activation.getRunForCategories()).size() > 0)
                && (activation.getRunForUserTypes().isEmpty() || activation.getRunForUserTypes().contains(userType));
    }

    protected abstract List<FilterFeedback> doFilter(MessageProcessingContext context);
}
