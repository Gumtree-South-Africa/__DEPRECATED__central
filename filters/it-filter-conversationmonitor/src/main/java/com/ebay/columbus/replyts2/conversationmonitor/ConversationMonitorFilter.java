package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by fmaffioletti on 25/07/14.
 */
public class ConversationMonitorFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationMonitorFilter.class);

    protected static final String CONVERSATION_MONITOR_HINT = "CONVERSATION_MONITOR_HINT";
    protected static final String CONVERSATION_MONITOR_REASON = "CONVERSATION_MONITOR_PLUGIN";

    private Long warnSizeThreshold;
    private Long errorSizeThreshold;
    private List<String> triggerCharsList;
    private boolean thresholdCheckEnabled;

    private Map<String, Integer> triggerCharsMatched;

    public ConversationMonitorFilter(Long warnSizeThreshold, Long errorSizeThreshold,
                    List<String> triggerCharsList, boolean thresholdCheckEnabled) {
        this.warnSizeThreshold = warnSizeThreshold;
        this.errorSizeThreshold = errorSizeThreshold;
        this.triggerCharsList = triggerCharsList;
        this.thresholdCheckEnabled = thresholdCheckEnabled;
        this.triggerCharsMatched = Maps.newHashMap();
        for (String triggerChar : triggerCharsList) {
            triggerCharsMatched.put(triggerChar, 0);
        }
    }

    private void incrementTriggerCharCount(String triggerChar, Integer amount) {
        triggerCharsMatched.put(triggerChar, triggerCharsMatched.get(triggerChar) + amount);
    }

    @Override public List<FilterFeedback> filter(MessageProcessingContext context) {
        LOG.debug("Applying ConversationMonitor filter");
        ImmutableList.Builder<FilterFeedback> filterFeedbackList = ImmutableList.builder();

        Long sizeOfConversation = context.getConversation().getMessages().stream().mapToLong(message -> message.getPlainTextBody().length()).sum();


        context.getConversation().getMessages().stream().forEach(message ->
            triggerCharsList.stream().
                    filter(triggerChar -> message.getPlainTextBody().contains(triggerChar)).
                    forEach(triggerChar -> {
                        incrementTriggerCharCount(triggerChar,
                        StringUtils.countOccurrencesOf(message.getPlainTextBody(), triggerChar));
                        LOG.error("Conversation with ID [" + context.getConversation().getId()
                                + "] created at [" + context.getConversation().getCreatedAt()
                                + "] contains a trigger char [" + triggerChar
                                + "] in message with ID [" + message.getId() + "]");
            })
        );

        triggerCharsMatched.keySet().stream().filter(triggerChar -> triggerCharsMatched.get(triggerChar) > 0).forEach(triggerChar -> {
            LOG.error("Conversation with ID [" + context.getConversation().getId()
                    + "] created at [" + context.getConversation().getCreatedAt()
                    + "] contains a total of [" + triggerCharsMatched.get(triggerChar)
                    + "] of this trigger char [" + triggerChar + "]");
        });

        triggerCharsMatched.keySet().stream().forEach( triggerChar  -> triggerCharsMatched.put(triggerChar, 0));

        LOG.debug("Conversation with ID [" + context.getConversation().getId() + "] created at ["
                        + context.getConversation().getCreatedAt() + "] has size ["
                        + sizeOfConversation + "]");

        if (thresholdCheckEnabled) {
            if (errorSizeThreshold != null && sizeOfConversation > errorSizeThreshold) {
                LOG.error("Conversation with ID [" + context.getConversation().getId()
                                + "] has size [" + sizeOfConversation
                                + "] greater than error threshold [" + errorSizeThreshold + "]");
                filterFeedbackList.add(new FilterFeedback(CONVERSATION_MONITOR_HINT,
                                CONVERSATION_MONITOR_REASON
                                                + " found a conversation with size greater than error threshold",
                                0, FilterResultState.OK));

            } else if (warnSizeThreshold != null && sizeOfConversation > warnSizeThreshold) {
                LOG.warn("Conversation with ID [" + context.getConversation().getId()
                                + "] has size [" + sizeOfConversation
                                + "] greater than the warn threshold [" + warnSizeThreshold + "]");
                filterFeedbackList.add(new FilterFeedback(CONVERSATION_MONITOR_HINT,
                                CONVERSATION_MONITOR_REASON
                                                + " found a conversation with size greater than warning threshold",
                                0, FilterResultState.OK));

            }
        }

        return Collections.emptyList();
    }

}
