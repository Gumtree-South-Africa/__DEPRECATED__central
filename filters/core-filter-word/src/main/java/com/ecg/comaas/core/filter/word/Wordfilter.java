package com.ecg.comaas.core.filter.word;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

class Wordfilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(Wordfilter.class);

    static final String CATEGORY_ID = "categoryid";

    private static final Timer REGEX_TIMER = TimingReports.newTimer("core.wordfilter.regex.timing");
    private static final Counter SKIPPED_REGEX_COUNTER = TimingReports.newCounter("core.wordfilter.regex.counter.skipped");

    private final List<PatternEntry> patterns;
    private final boolean ignoreDuplicatePatterns;
    private final long regexProcessingTimeoutMs;


    Wordfilter(FilterConfig filterConfig, long regexProcessingTimeoutMs) {
        this.regexProcessingTimeoutMs = regexProcessingTimeoutMs;
        this.patterns = filterConfig.getPatterns();
        this.ignoreDuplicatePatterns = filterConfig.isIgnoreQuotedPatterns();
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String processText = getProcessText(messageProcessingContext);
        Optional<String> categoryId = getCategoryId(messageProcessingContext);
        return filterByPatterns(processText, messageProcessingContext, categoryId.orElse(null));
    }

    private Optional<String> getCategoryId(MessageProcessingContext messageProcessingContext) {
        return Optional.ofNullable(messageProcessingContext.getConversation().getCustomValues().get(CATEGORY_ID));
    }

    private String getProcessText(MessageProcessingContext messageProcessingContext) {
        if (messageProcessingContext.getMail().isPresent()) {
            return messageProcessingContext.getMail().get().getSubject()
                + " " + messageProcessingContext.getMessage().getPlainTextBody();
        }
        else {
            return messageProcessingContext.getMessage().getPlainTextBody();
        }
    }

    private List<FilterFeedback> filterByPatterns(String processText, MessageProcessingContext context, String conversationCategoryId) {
        Builder<FilterFeedback> feedbacks = ImmutableList.builder();

        Set<String> foundPatterns = ignoreDuplicatePatterns ? new PreviousPatternsExtractor(context.getConversation(), context.getMessage()).previouselyFiredPatterns() : Collections.emptySet();

        for (PatternEntry patternEntry : patterns) {
            // Check if total processing time has been exceeded.
            // This should protect us from DoS attacks where RegEx processing took very long.
            context.getProcessingTimeGuard().check();

            if (patternEntry.getCategoryIds().isEmpty() || (conversationCategoryId != null && patternEntry.getCategoryIds().contains(conversationCategoryId))) {
                Matcher matcher = new ExpiringRegEx(processText, patternEntry.getPattern(), regexProcessingTimeoutMs, context.getConversation().getId(), context.getMessageId()).createMatcher();
                applyPattern(feedbacks, patternEntry, matcher, foundPatterns);
            }
        }

        return feedbacks.build();
    }


    private void applyPattern(Builder<FilterFeedback> feedbacks, PatternEntry p, Matcher matcher, Set<String> previouselyFoundPatterns) {
        boolean foundMatch = false;
        try (Context ignore = REGEX_TIMER.time()) {
            foundMatch = matcher.find();
        } catch (RuntimeException e) {
            LOG.info("Skipping Regular Expression '" + p.getPattern() + "': ", e);
            SKIPPED_REGEX_COUNTER.inc();
        }

        if (foundMatch) {
            String description = "Matched word " + matcher.group();
            String currentRegexp = p.getPattern().toString();
            int score = previouselyFoundPatterns.contains(currentRegexp) ? 0 : p.getScore();
            feedbacks.add(
                    new FilterFeedback(
                            currentRegexp,
                            description,
                            score,
                            FilterResultState.OK));
        }
    }

}
