package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static java.lang.String.format;

public class Wordfilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(Wordfilter.class);

    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    private static final String CATEGORY_ID = "categoryid";

    private List<PatternEntry> patterns;

    private boolean ignoreDuplicatePatterns;

    public Wordfilter(FilterConfig filterConfig) {
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
          Optional.of(messageProcessingContext.getConversation().getCustomValues().get(CATEGORY_ID)) : Optional.empty();
    }

    private String getProcessText(MessageProcessingContext messageProcessingContext) {
        return messageProcessingContext.getMessage().getPlainTextBody();
    }

    private List<FilterFeedback> filterByPatterns(String processText, MessageProcessingContext context, Optional<String> conversationCategoryId) {
        List<FilterFeedback> result = new ArrayList<>();

        Set<String> foundPatterns = ignoreDuplicatePatterns  ? new PreviousPatternsExtractor(context.getConversation(), context.getMessage()).previouselyFiredPatterns() : Collections.<String>emptySet();

        for (PatternEntry patternEntry : patterns) {
            if (!patternEntry.getCategoryIds().isPresent() || (conversationCategoryId.isPresent() && patternEntry.getCategoryIds().get().contains(conversationCategoryId.get()))){
                Matcher matcher = new ExpiringRegEx(processText, patternEntry.getPattern(), TIMEOUT, context.getConversation().getId(), context.getMessageId()).createMatcher();

                applyPattern(result, patternEntry, matcher, foundPatterns).ifPresent(result::add);
            }
        }

        return Collections.unmodifiableList(result);
    }

    private Optional<FilterFeedback> applyPattern(List<FilterFeedback> result, PatternEntry pattern, Matcher matcher, Set<String> previouselyFoundPatterns) {
        try {
            if (matcher.find()) {
                String description = format("Matched word %s", matcher.group());
                int score = previouselyFoundPatterns.contains(pattern.getPattern().toString()) ? 0 : pattern.getScore();

                return Optional.of(new FilterFeedback(pattern.getPattern().toString(), description, score, FilterResultState.OK));
            }
        } catch (RuntimeException e) {
            LOG.error("Skipping Regular Expression '{}'", pattern, e);
        }

        return Optional.empty();
    }
}