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

import static java.lang.String.format;

@Component
class FilterListProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FilterListProcessor.class);

    @Autowired
    @Qualifier("filterConfigurationAdmin")
    private ConfigurationAdmin<Filter> filterConfig;

    @Autowired
    private FilterListMetrics metrics;

    List<ProcessingFeedback> processAllFilters(MessageProcessingContext context) throws ProcessingTimeExceededException {
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
                List<FilterFeedback> filterFeedback = filter.filter(context);
                if (filterFeedback != null) {

                    // if this filter returns an ACCEPT_AND_TERMINATE, we can consider finished the processing
                    // of all the filters, and just return all the preceding feedbacks
                    if (filterFeedback.stream().anyMatch(feedback -> feedback.getResultState() == FilterResultState.ACCEPT_AND_TERMINATE)) {
                        return allFeedback;
                    }

                    allFeedback.addAll(adaptToPluginState(filterReference, filterFeedback));
                }
            } catch (RuntimeException e) {
                String errorMessage = format("Filter %s failed on Message '%s' (ConvId: '%s')", filterReference, context.getMessageId(), context.getConversation().getId());
                LOG.error(errorMessage, e);
            }
        }
        return allFeedback;
    }

    private List<ProcessingFeedback> adaptToPluginState(PluginInstanceReference<Filter> pc, List<FilterFeedback> filterFeedback) {
        List<ProcessingFeedback> converted = new ArrayList<>();
        for (FilterFeedback p : filterFeedback) {
            ConfigurationId configurationId = pc.getConfiguration().getId();
            converted.add(
                    new ImmutableProcessingFeedback(
                            configurationId.getPluginFactory(),
                            configurationId.getInstanceId(),
                            p.getUiHint(),
                            p.getDescription(),
                            p.getScore(),
                            p.getResultState(),
                            pc.getState() == PluginState.EVALUATION)
            );
        }
        return converted;
    }
}
