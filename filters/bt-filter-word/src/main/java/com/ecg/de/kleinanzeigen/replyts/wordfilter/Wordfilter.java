package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

class Wordfilter implements Filter {

    public static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    static final String CATEGORY_ID = "categoryid";

    private static final Logger LOG = LoggerFactory.getLogger(Wordfilter.class);

    private final List<PatternEntry> patterns;
    private final boolean ignoreDuplicatePatterns;

    Wordfilter(FilterConfig filterConfig) {
        this.patterns = filterConfig.getPatterns();
        this.ignoreDuplicatePatterns = filterConfig.isIgnoreQuotedPatterns(); 
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        String processText = getProcessText(messageProcessingContext);
        Optional<String> categoryId = getCategoryId(messageProcessingContext);
        return filterByPatterns(processText, messageProcessingContext, categoryId);
    }

    private Optional<String> getCategoryId(MessageProcessingContext messageProcessingContext) {
        return messageProcessingContext.getConversation().getCustomValues().containsKey(CATEGORY_ID) ?
                Optional.of(messageProcessingContext.getConversation().getCustomValues().get(CATEGORY_ID)) :
                Optional.<String>absent();
    }

    private String getProcessText(MessageProcessingContext messageProcessingContext) {
        //return messageProcessingContext.getMail().getSubject()
        //        + " " + messageProcessingContext.getMessage().getPlainTextBody();
        return messageProcessingContext.getMessage().getPlainTextBody();
    }

    private List<FilterFeedback> filterByPatterns(String processText, MessageProcessingContext context, Optional<String> conversationCategoryId) {
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.<FilterFeedback>builder();

        Set<String> foundPatterns = ignoreDuplicatePatterns  ? new PreviousPatternsExtractor(context.getConversation(), context.getMessage()).previouselyFiredPatterns() : Collections.<String>emptySet();

        for (PatternEntry patternEntry : patterns) {
            if(!patternEntry.getCategoryIds().isPresent() ||
                    (conversationCategoryId.isPresent() && patternEntry.getCategoryIds().get().contains(conversationCategoryId.get()))){
                Matcher matcher = new ExpiringRegEx(processText, patternEntry.getPattern(), TIMEOUT, context.getConversation().getId(), context.getMessageId()).createMatcher();
                applyPattern(feedbacks, patternEntry, matcher, foundPatterns);
            }
        }

        return feedbacks.build();
    }



    private void applyPattern(Builder<FilterFeedback> feedbacks, PatternEntry p, Matcher matcher, Set<String> previouselyFoundPatterns) {
        boolean foundMatch = false;
        try {
            foundMatch = matcher.find();
        } catch (RuntimeException e) {
            LOG.error("Skipping Regular Expression '"+p.getPattern()+"': ", e);
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