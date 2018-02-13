package com.ecg.replyts.commonattributefilter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import static java.lang.String.format;

public class CommonAttributeFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(CommonAttributeFilter.class);

    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private List<PatternEntry> patterns;

    private String attribute;

    public CommonAttributeFilter(FilterConfig filterConfig) {
        this.patterns = filterConfig.getPatterns();
        this.attribute = filterConfig.getAttribute();
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String processText = getProcessText(messageProcessingContext);
        if(processText == null || processText.isEmpty()) return null;
        return filterByPatterns(processText, messageProcessingContext);
    }

    private String getProcessText(MessageProcessingContext messageProcessingContext) {
    	Map<String, String> cvs = messageProcessingContext.getConversation().getCustomValues();
    	return cvs.get(attribute);
    }

    private List<FilterFeedback> filterByPatterns(String processText, MessageProcessingContext context) {
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.<FilterFeedback>builder();

        for (PatternEntry p : patterns) {
            Matcher matcher = new ExpiringRegEx(processText, p.getPattern(), TIMEOUT, context.getConversation().getId(), context.getMessageId()).createMatcher();
            applyPattern(feedbacks, p, matcher);
        }

        return feedbacks.build();
    }

    private void applyPattern(Builder<FilterFeedback> feedbacks, PatternEntry pattern, Matcher matcher) {
        try {
            if (matcher.find()) {
                String description = format("Matched attribute %s", matcher.group());

                feedbacks.add(new FilterFeedback(pattern.getPattern().toString(), description, pattern.getScore(), FilterResultState.OK));
            }
        } catch (RuntimeException e) {
            LOG.error("Skipping Regular Expression '{}'", pattern.getPattern(), e);

            return;
        }
    }
}