package com.ecg.replyts.app.filterchain;

import com.codahale.metrics.Counter;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.command.MessageFilteredCommand;
import com.ecg.replyts.core.api.model.conversation.command.MessageFilteredCommandBuilder;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import com.ecg.replyts.core.runtime.configadmin.PluginInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.String.format;

@Component
public class FilterChain {
    private static final Logger LOG = LoggerFactory.getLogger(FilterChain.class);

    private static final String TERMINATION_TEMPLATE = "FilterChain ended up in result state %s";
    private static final String DISCARDING_TEMPLATE = "FilterChain stopped, processing time exceeded: %s";
    private static final Counter PROCESSING_TIME_EXCEEDED_COUNTER = TimingReports.newCounter("processing-exceeded");

    @Autowired
    @Qualifier("resultInspectorConfigAdmin")
    private ConfigurationAdmin<ResultInspector> resultInspectorConfig;

    @Autowired
    private FilterListProcessor filterListProcessor;

    @Autowired
    private HeldMailRepository heldMailRepository;

    public void filter(MessageProcessingContext context) {
        LOG.debug("Filtering message {}", context.getMessageId());

        try {
            List<ProcessingFeedback> allFeedback = filterListProcessor.processAllFilters(context);

            inspectProcessingFeedback(allFeedback);

            FilterResultState overallResultState = findOverallResultState(allFeedback);

            MessageFilteredCommand messageFilteredCommand = MessageFilteredCommandBuilder.
                    aMessageFilteredCommand(
                            context.getConversation().getId(),
                            context.getMessageId())
                    .withState(overallResultState)
                    .withProcessingFeedback(allFeedback).build();

            context.addCommand(messageFilteredCommand);

            MessageState terminationState = getTerminationStateFrom(overallResultState);
            if (terminationState != null) {
                if (terminationState.equals(MessageState.HELD) || terminationState.equals(MessageState.BLOCKED)) {
                    heldMailRepository.write(context.getMessageId(), Mails.writeToBuffer(context.getMail()));
                }

                LOG.debug("Terminating Message {} with state {}", context.getMessageId(), terminationState);
                context.terminateProcessing(terminationState, this, format(TERMINATION_TEMPLATE, overallResultState));
            }
        } catch (ProcessingTimeExceededException e) {
            PROCESSING_TIME_EXCEEDED_COUNTER.inc();
            context.terminateProcessing(MessageState.DISCARDED, this, format(DISCARDING_TEMPLATE, e.getMessage()));
            LOG.warn(format("Processing time exceeded, message %s DISCARDED", context.getMessageId()), e);
        }
    }

    private void inspectProcessingFeedback(List<ProcessingFeedback> allFeedback) {
        for (PluginInstanceReference<ResultInspector> inspectorRef : resultInspectorConfig.getRunningServices()) {
            LOG.debug("Applying Result Inspector {}", inspectorRef);
            ResultInspector resultInspector = inspectorRef.getCreatedService();
            resultInspector.inspect(allFeedback);
        }
    }

    private FilterResultState findOverallResultState(List<ProcessingFeedback> allFeedback) {
        FilterResultState overallResultState = FilterResultState.OK;
        for (ProcessingFeedback feedback : allFeedback) {
            LOG.debug("Processing Feedback from {}:{}. Score {}, State {}. Evaluation: {} ", feedback.getFilterName(), feedback.getFilterName(), feedback.getScore(), feedback.getResultState(), feedback.isEvaluation());
            if (!feedback.isEvaluation()) {
                FilterResultState resultState = feedback.getResultState();
                if (overallResultState.isTransitionByFiltersAllowedTo(resultState)) {
                    overallResultState = resultState;
                }
            }
        }
        return overallResultState;
    }

    private static MessageState getTerminationStateFrom(FilterResultState filterResultState) {
        switch (filterResultState) {
            case HELD:
                return MessageState.HELD;
            case DROPPED:
                return MessageState.BLOCKED;
            default:
                return null;
        }
    }
}
