package com.ecg.replyts.core.api.pluginconfiguration.filter;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;

import java.util.List;

/**
 * If this plugin is not {@link com.ecg.replyts.core.api.pluginconfiguration.PluginState#DISABLED}, every filter
 * instance will be put into the filter chain and called on every mailthat runs through ReplyTS. <br /> If this Filter
 * is ni {@link com.ecg.replyts.core.api.pluginconfiguration.PluginState#EVALUATION}, the processing feedback will be
 * ignored by the filter chain. it will only be visible to cs agents.
 *
 * @author mhuttar
 */
public interface Filter {
    /**
     * Applied on a message to filter it. The generated Processing Feedback will be stored within the message an made
     * available via API for screening agents.
     *
     * @param context processing context that contains all message related data (the message, the original mail, the
     *                conversation with all other messages in it,...)
     * @return a list of {@link FilterFeedback} this filter has for the given message. Return one Filter
     * Feedback per violated rule. The ui hint of the filter feedback should be somewhat machine parseable
     * (e.g. a regular expression that matched, or a threshold that was exceeded). the descriptions should be
     * understandable by CS agents.
     * @throws ProcessingTimeExceededException Long running filter implementation should check continuously the total processing
     *                                         time with {@link ProcessingTimeGuard#check()}
     */
    List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException;
}
