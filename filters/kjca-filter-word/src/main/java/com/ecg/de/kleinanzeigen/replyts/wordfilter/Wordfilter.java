package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.comaas.core.filter.activable.ActivableFilter;
import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Wordfilter extends ActivableFilter {
    private static final Logger LOG = LoggerFactory.getLogger(Wordfilter.class);

    static final String CATEGORY_ID = "categoryid";

    private final List<PatternEntry> patterns;
    private final boolean ignoreDuplicatePatterns;
    private final boolean ignoreFollowUps;
    private final long regexProcessingTimeoutMs;

    Wordfilter(FilterConfig filterConfig, Activation activation, long regexProcessingTimeoutMs) {
        super(activation);

        this.patterns = filterConfig.getPatterns();
        this.ignoreDuplicatePatterns = filterConfig.isIgnoreQuotedPatterns();
        this.ignoreFollowUps = filterConfig.isIgnoreFollowUps();
        this.regexProcessingTimeoutMs = regexProcessingTimeoutMs;
    }

    @Override
    protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
        if (ignoreFollowUps && haveResponseFromNonInitiatingParty(context)) {
            /*
            If the conversation has at least one response from the other party, it means that
            every message is now a "follow-up", and we can stop filtering. Otherwise, spammers
            will just send 2+ messages via the platform to bypass this filter.
            */
            LOG.trace("Ignoring follow-up in conv id [{}], msg id [{}]", context.getConversation().getId(), context.getMessageId());

            return Collections.emptyList();
        }

        String processText = getProcessText(context);
        Optional<String> categoryId = getCategoryId(context);
        return filterByPatterns(processText, context, categoryId);
    }

    private boolean haveResponseFromNonInitiatingParty(MessageProcessingContext context) {
        List<Message> messages = context.getConversation().getMessages();

        if (messages.size() == 0) {
            return false;
        }

        MessageDirection firstMessageDirection = messages.get(0).getMessageDirection();

        ListIterator<Message> it = messages.listIterator(1);
        while (it.hasNext()) {
            if (!it.next().getMessageDirection().equals(firstMessageDirection)) {
                return true;
            }
        }

        return false;
    }

    private Optional<String> getCategoryId(MessageProcessingContext messageProcessingContext) {
        return messageProcessingContext.getConversation().getCustomValues().containsKey(CATEGORY_ID) ?
                Optional.of(messageProcessingContext.getConversation().getCustomValues().get(CATEGORY_ID)) :
                Optional.empty();
    }

    private String getProcessText(MessageProcessingContext messageProcessingContext) {
        return messageProcessingContext.getMail().get().getSubject()
                + " " + messageProcessingContext.getMessage().getPlainTextBody();
    }

    private List<FilterFeedback> filterByPatterns(String processText, MessageProcessingContext context, Optional<String> conversationCategoryId) {
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();

        Set<String> foundPatterns = ignoreDuplicatePatterns ? new PreviousPatternsExtractor(context.getConversation(), context.getMessage()).previouslyFiredPatterns() : Collections.emptySet();

        for (PatternEntry patternEntry : patterns) {
            // Check if total processing time has been exceeded.
            // This should protect us from DoS attacks where RegEx processing took very long.
            context.getProcessingTimeGuard().check();

            if (patternEntry.getCategoryIds().isEmpty() ||
                    (conversationCategoryId.isPresent() && patternEntry.getCategoryIds().contains(conversationCategoryId.get()))) {

                applyPattern(processText, context, feedbacks, foundPatterns, patternEntry);
            }
        }

        return feedbacks.build();
    }

    private void applyPattern(
            String processText,
            MessageProcessingContext context,
            ImmutableList.Builder<FilterFeedback> feedbacks,
            Set<String> previouslyFoundPatterns,
            PatternEntry patternEntry
    ) {
        Pattern pattern = patternEntry.getPattern();
        String conversationId = context.getConversation().getId();
        String messageId = context.getMessageId();
        Matcher matcher = new ExpiringRegEx(processText, pattern, regexProcessingTimeoutMs, conversationId, messageId).createMatcher();

        try {
            if (matcher.find()) {
                String description = "Matched word " + matcher.group();
                String currentRegexp = patternEntry.getPattern().toString();
                int score = previouslyFoundPatterns.contains(currentRegexp) ? 0 : patternEntry.getScore();
                feedbacks.add(
                        new FilterFeedback(
                                currentRegexp,
                                description,
                                score,
                                FilterResultState.OK));
            }
        } catch (RuntimeException | StackOverflowError e) {
            LOG.warn("Skipping Regular Expression '{}' on conv/msg '{}/{}'. {}", pattern, conversationId, messageId, e.toString());
        }
    }
}
