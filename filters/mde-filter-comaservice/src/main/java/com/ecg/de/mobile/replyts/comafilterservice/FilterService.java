package com.ecg.de.mobile.replyts.comafilterservice;

import com.ecg.de.mobile.replyts.comafilterservice.filters.ComaFilterService;
import com.ecg.de.mobile.replyts.comafilterservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FilterService implements Filter {

    public static final String CUSTOM_HEADER_PREFIX = "X-Cust-";
    static final String CUSTOM_HEADER_MESSAGE_TYPE = CUSTOM_HEADER_PREFIX + "Message_Type";
    static final String CUSTOM_HEADER_BUYER_TYPE = CUSTOM_HEADER_PREFIX + "Buyer_Type";
    static final String MESSAGE_TYPE_CONVERSATION = "CONVERSATION_MESSAGE";

    static final String DEALER = "DEALER";
    private final ComaFilterService comaFilterService;
    private final ContactMessageAssembler contactMessageAssembler;


    private static final Logger LOGGER = LoggerFactory.getLogger(FilterService.class);


    FilterService(ComaFilterService comaFilterService, ContactMessageAssembler contactMessageAssembler) {
        this.comaFilterService = comaFilterService;
        this.contactMessageAssembler = contactMessageAssembler;
    }

    /**
     * Applied on a message to filter it. The generated Processing Feedback will be stored within the message an made
     * available via API for screening agents.
     *
     * @param context processing context that contains all message related data (the message, the original mail, the
     *                conversation with all other messages in it,...)
     * @return a list of {@link FilterFeedback} this filter has for the given message. Return one Filter
     * Feedback per violated rule. The uihint of the filter feedback should be somewhat machine parsable
     * (e.g. a regular expression that matched, or a threshold that was exceeded). the descriptions should be
     * understandable by CS agents.
     */
    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        return applyFiltersToMessage(context)
                .stream()
                .map(FilterService::toFilterFeedback)
                .collect(Collectors.toList());
    }

    private static FilterFeedback toFilterFeedback(String alertingFilter) {
        return new FilterFeedback(
                alertingFilter,
                "Coma Filter Service alert: Filter " + alertingFilter + " matched.",
                0,
                FilterResultState.DROPPED
        );
    }

    private Collection<String> applyFiltersToMessage(MessageProcessingContext messageContext) {
        // do not check messages coming from dealers
        if (isDealerBuyer(messageContext)) {
            LOGGER.info("Do not use filter service on mail to adid {}: buyer is dealer.", messageContext.getMail().getAdId());
            return Collections.emptySet();
        }

        try {
            if (isConversationMessage(messageContext)) {
                LOGGER.info("Filter service for conversation on ADID={}", messageContext.getMail().getAdId());
                return comaFilterService.getFilterResultsForConversation(contactMessageAssembler.getContactMessage(messageContext));
            } else {
                LOGGER.info("Filter service for message on ADID={}", messageContext.getMail().getAdId());
                return comaFilterService.getFilterResultsForMessage(contactMessageAssembler.getContactMessage(messageContext));
            }

        } catch (Exception e) {
            LOGGER.error("Filter service failed: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    private boolean isDealerBuyer(MessageProcessingContext context) {
        String buyerType = context.getMessage().getHeaders().get(CUSTOM_HEADER_BUYER_TYPE);
        return DEALER.equals(buyerType);
    }

    private boolean isConversationMessage(MessageProcessingContext context) {
        return MESSAGE_TYPE_CONVERSATION.equalsIgnoreCase(context.getMessage().getHeaders().get(CUSTOM_HEADER_MESSAGE_TYPE));
    }

}
