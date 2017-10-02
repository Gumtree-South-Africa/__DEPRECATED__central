package com.ecg.replyts.app.filterchain;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import com.ecg.replyts.core.runtime.configadmin.PluginInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class FilterListProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FilterListProcessor.class);

    @Autowired
    @Qualifier("filterConfigurationAdmin")
    private ConfigurationAdmin<Filter> filterConfig;

    @Autowired
    private FilterListMetrics metrics;

    List<ProcessingFeedback> processAllFilters(MessageProcessingContext context)
            throws ProcessingTimeExceededException {
        List<ProcessingFeedback> allFeedback = new ArrayList<>();
        ProcessingTimeGuard timeGuard = context.getProcessingTimeGuard();

        for (PluginInstanceReference<Filter> filterReference : filterConfig.getRunningServices()) {

            PluginState pluginState = filterReference.getState();
            if (pluginState == PluginState.DISABLED) {
                LOG.trace("Skipping Filter {}", filterReference);
                continue;
            }
            // break processing if total processing time has been exceeded
            timeGuard.check();

            LOG.trace("Applying Filter {}", filterReference);
            Filter filter = filterReference.getCreatedService();
            ConfigurationId filterId = filterReference.getConfiguration().getId();
            try (Timer.Context ignore = metrics.newOrExistingTimerFor(filterId)) {
                List<FilterFeedback> filterFeedbackList = filter.filter(context);
                if (filterFeedbackList != null) {

                    if (hasAcceptAndTerminate(filterFeedbackList)) {
                        return allFeedback;
                    }

                    ConfigurationId configurationId = filterReference.getConfiguration().getId();
                    boolean evaluation = filterReference.getState() == PluginState.EVALUATION;
                    for (FilterFeedback filterFeedback : filterFeedbackList) {
                        ProcessingFeedback processingFeedback = adaptToPluginState(
                            configurationId,
                            evaluation,
                            filterFeedback);
                        allFeedback.add(processingFeedback);
                    }
                }
            } catch (RuntimeException exception) {
                LOG.error(
                    "Filter {} failed on Message '{}' (ConvId: '{}')",
                    filterReference,
                    context.getMessageId(),
                    context.getConversation().getId(),
                    exception);
            }
        }
        return allFeedback;
    }

    private boolean hasAcceptAndTerminate(List<FilterFeedback> filterFeedbackList) {
        for (FilterFeedback filterFeedback : filterFeedbackList) {
            if (filterFeedback.getResultState() == FilterResultState.ACCEPT_AND_TERMINATE) {
                return true;
            }
        }
        return false;
    }

    private ProcessingFeedback adaptToPluginState(ConfigurationId configurationId, boolean evaluation,
            FilterFeedback filterFeedback) {
        return new ImmutableProcessingFeedback(configurationId.getPluginFactory(), configurationId.getInstanceId(),
                filterFeedback.getUiHint(), filterFeedback.getDescription(), filterFeedback.getScore(),
                filterFeedback.getResultState(), evaluation);
    }
}