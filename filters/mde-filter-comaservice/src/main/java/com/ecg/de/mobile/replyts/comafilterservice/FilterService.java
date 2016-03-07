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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FilterService implements Filter {

    public static final String CUSTOM_HEADER_PREFIX = "X-Cust-";
    
    static final String DEALER = "DEALER";
    private final ComaFilterService comaFilterService;
    private final ContactMessageAssembler contactMessageAssembler;
    
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterService.class);

    
    public FilterService(@Value("${replyts.mobile.comafilterservice.webserviceUrl}") String comaFilterServiceUrl,
                         @Value("${replyts.mobile.comafilterservice.active}") boolean isActive) {
        comaFilterService = new ComaFilterService(comaFilterServiceUrl, isActive);
        contactMessageAssembler = new ContactMessageAssembler();
        
    }
    
    FilterService(ComaFilterService comaFilterService) {
        this.comaFilterService = comaFilterService;
        contactMessageAssembler = new ContactMessageAssembler();
    }

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

        Collection<String> results = applyFiltersToMessage(context);
        
        ArrayList<FilterFeedback> filterFeedbackList = new ArrayList<FilterFeedback>();
        FilterFeedback filter;
        for(String alertingFilter : results) {
            filter = new FilterFeedback(alertingFilter,"Coma Filter Service alert: Filter "+alertingFilter+" matched.",0,FilterResultState.DROPPED);
            filterFeedbackList.add(filter);
        }

        return filterFeedbackList;
    }

    private Collection<String> applyFiltersToMessage(MessageProcessingContext messageContext) {
        
        /**
         * Do only check first message in conversation.
         */
        if(!isInitialMessage(messageContext)) {
            LOGGER.info("Do not use filter service on mail to adid {}: no initial message.",messageContext.getMail().getAdId());
            return Collections.emptySet();
        }

        /**
         * do not check messages coming from dealers
         */
        if (isDealerBuyer(messageContext)) {
            LOGGER.info("Do not use filter service on mail to adid {}: buyer is dealer.",messageContext.getMail().getAdId());
               return Collections.emptySet();
        }

        LOGGER.info("Filter service on adid {}",messageContext.getMail().getAdId());
        try{
            return comaFilterService.getFilterResults(contactMessageAssembler.getContactMessage(messageContext));
            
        } catch(Exception e){ // we are not important (yet)
            LOGGER.error("Filter service failed.");
            LOGGER.debug("Filter service failed. .",e);
            return Collections.emptySet();
        }
    }


    private boolean isInitialMessage(MessageProcessingContext context){
        /**
         * Search for any header field that coma service sets. 
         * If it is set, the message comes from coma so it's an initial message.     
         */
        return context.getMessage().getHeaders().containsKey(CUSTOM_HEADER_PREFIX + "Seller_Type");
    }

    private boolean isDealerBuyer(MessageProcessingContext context){
        String buyerType = context.getMessage().getHeaders().get(CUSTOM_HEADER_PREFIX + "Buyer_Type");
        return DEALER.equals(buyerType);
    }


}
