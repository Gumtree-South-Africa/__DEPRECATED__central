package com.ecg.gumtree.comaas.filter.volume;

import com.codahale.metrics.Timer;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.longDescription;
import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.resultFilterResultMap;

@Component
public class GumtreeVolumeFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeVolumeFilter.class);

    private static final Timer TIMER = TimingReports.newTimer("volume-filter-process-time");
    static final String MARKED_SEEN_BY_VOLUME_FILTER = "markedSeenByVolumeFilter";

    private Filter pluginConfig;
    private VelocityFilterConfig volumeFilterConfig;

    private VolumeFilterServiceHelper volumeFilterServiceHelper;
    private SearchService searchService;
    private EventStreamProcessor eventStreamProcessor;
    private String instanceName;
    private SharedBrain sharedBrain;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) throws ProcessingTimeExceededException {
        try (Timer.Context ignore = TIMER.time()) {
            logMailReceivedEvent(messageContext);

            VelocityFilterConfig.FilterField volumeFilterField = volumeFilterConfig.getFilterField();
            String value = extractValue(volumeFilterField, messageContext.getMessage(), messageContext.getConversation());

            if (GumtreeFilterUtil.hasExemptedCategory(volumeFilterConfig, messageContext)) {
                LOG.trace("Not filtering because has exempted category");
                return Collections.emptyList();
            }

            if (canBypassFilter(volumeFilterConfig, messageContext, volumeFilterField, value)) {
                return Collections.emptyList();
            }

            LOG.trace("Proceeding to actual filter");
            return processVolumeFilter(volumeFilterConfig, value, instanceName);
        }
    }

    private void logMailReceivedEvent(MessageProcessingContext context) {
        Object markedSeenByVolumeFilter = context.getFilterContext().getOrDefault(MARKED_SEEN_BY_VOLUME_FILTER, false);
        if (markedSeenByVolumeFilter == null) {
            markedSeenByVolumeFilter = false;
        }

        LOG.trace("Mail was {}marked as already seen by volume filter {}", ((boolean) markedSeenByVolumeFilter ? "" : "not "), instanceName);

        if ((boolean) markedSeenByVolumeFilter) {
            return;
        }

        if (!isFirstMessage(context.getMessage(), context.getConversation())) {
            LOG.trace("Not marking as seen because not first message for volume filter {}", instanceName);
            return;
        }
        Conversation conversation = context.getConversation();
        Message message = context.getMessage();

        String emailAddress = conversation.getBuyerId();
        String ipAddress = message.getHeaders().get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());
        String cookieId = message.getHeaders().get(GumtreeCustomHeaders.BUYER_COOKIE.getHeaderValue());
        sharedBrain.markSeen(emailAddress, ipAddress, cookieId);
        context.getFilterContext().put(MARKED_SEEN_BY_VOLUME_FILTER, true);
    }

    private List<FilterFeedback> processVolumeFilter(VelocityFilterConfig volumeFilterConfig, String value, String instanceName) {
        int seconds = volumeFilterConfig.getSeconds();
        int messageThreshold = volumeFilterConfig.getMessages();
        long mailsInTimeWindow = eventStreamProcessor.count(value, instanceName);

        LOG.debug("Num of mails in {} {}: {}, - for volume field {}, instance {}", seconds, "seconds", mailsInTimeWindow, value, instanceName);

        List<FilterFeedback> reasons = new ArrayList<>();
        boolean volumeExceeded = mailsInTimeWindow > messageThreshold;
        LOG.trace("boolean volumeExceeded = mailsInTimeWindow > messageThreshold; {} = {} > {}", volumeExceeded, mailsInTimeWindow, messageThreshold);

        if (volumeFilterConfig.isExceeding() && volumeExceeded) {
            LOG.debug("Volume exceeded. {} mails in time window of {} seconds for volume field {}, instance {}", seconds, "seconds", mailsInTimeWindow, value, instanceName);

            String shortDescription = String.format("More than %d messages in %d seconds", messageThreshold, seconds);
            addFilterFeedbackReason(volumeFilterConfig, value, reasons, shortDescription);
        } else if (!volumeFilterConfig.isExceeding() && !volumeExceeded) {
            LOG.debug("Volume too low. {} mails in time window of {} seconds for volume field {}, instance {}", seconds, "seconds", mailsInTimeWindow, value, instanceName);

            String shortDescription = String.format("Less than %d messages in %d seconds", messageThreshold, seconds);
            addFilterFeedbackReason(volumeFilterConfig, value, reasons, shortDescription);
        }
        return reasons;
    }

    private void addFilterFeedbackReason(VelocityFilterConfig filterConfig, String value,
                                         List<FilterFeedback> reasons, String shortDescription) {
        String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), shortDescription);
        reasons.add(new FilterFeedback(value, description, 0, resultFilterResultMap.get(filterConfig.getResult())));
    }

    private boolean isFirstMessage(Message message, Conversation conversation) {
        return message.getId().equals(conversation.getMessages().iterator().next().getId());
    }

    private boolean canBypassFilter(VelocityFilterConfig filterConfig, MessageProcessingContext messageContext,
                                    VelocityFilterConfig.FilterField filterField, String value) {
        if (!isFirstMessage(messageContext.getMessage(), messageContext.getConversation())) {
            LOG.debug(String.format("Second or greater message for this ad by this user. " +
                    "Bypassing volume filter for message id [%s]", messageContext.getMessageId()));
            return true;
        }

        if (isTemporarilyWhiteListed(filterField, value, filterConfig.getWhitelistSeconds())) {
            LOG.debug(String.format("Temporarily whitelisted user with volume field [%s]. " +
                    "Bypassing volume filter for message id [%s]", filterField, messageContext.getMessageId()));
            return true;
        }

        return false;
    }

    /**
     * Extract the value of the specified field from the given message.
     *
     * @param field   the field to extract
     * @param message the message to extract from
     * @return the value of the specified field
     */
    private String extractValue(VelocityFilterConfig.FilterField field, Message message, Conversation conversation) {
        String value = "";

        switch (field) {
            case EMAIL:
                value = conversation.getBuyerId();
                break;
            case COOKIE:
                value = message.getHeaders().get(GumtreeCustomHeaders.BUYER_COOKIE.getHeaderValue());
                break;
            case IP_ADDRESS:
                value = message.getHeaders().get(GumtreeCustomHeaders.BUYER_IP.getHeaderValue());
                break;
        }

        return value;
    }

    /**
     * Check if the message sender has been manually approved recently.
     *
     * @param field   field to identify the sender by
     * @param value   the sender identify to check
     * @param seconds number of seconds to check back through
     * @return true if the sender is white listed
     */
    private boolean isTemporarilyWhiteListed(VelocityFilterConfig.FilterField field, String value, int seconds) {
        SearchMessagePayload parameters = volumeFilterServiceHelper.createWhitelistSearchParameters(field, value, seconds);

        try {
            RtsSearchResponse result = searchService.search(parameters);

            if (result.getTotal() > 0) {
                return true;
            }
        } catch (PersistenceException e) {
            LOG.error(String.format("Could not search for messages matching field %s and value %s", field, value), e);
        }

        return false;
    }

    GumtreeVolumeFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeVolumeFilter withFilterConfig(VelocityFilterConfig filterConfig) {
        this.volumeFilterConfig = filterConfig;
        return this;
    }

    GumtreeVolumeFilter withSearchService(SearchService searchService) {
        this.searchService = searchService;
        return this;
    }

    GumtreeVolumeFilter withVolumeFilterServiceHelper(VolumeFilterServiceHelper helper) {
        this.volumeFilterServiceHelper = helper;
        return this;
    }

    GumtreeVolumeFilter withEventStreamProcessor(EventStreamProcessor eventStreamProcessor) {
        this.eventStreamProcessor = eventStreamProcessor;
        return this;
    }

    GumtreeVolumeFilter withInstanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    GumtreeVolumeFilter withSharedBrain(SharedBrain sharedBrain) {
        this.sharedBrain = sharedBrain;
        return this;
    }
}
