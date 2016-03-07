package com.ecg.de.mobile.replyts.comafilterservice;

import com.ecg.de.mobile.replyts.comafilterservice.filters.ComaFilterService;
import com.ecg.de.mobile.replyts.comafilterservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
class FilterServiceFactory implements FilterFactory {

    private final ComaFilterService comaFilterService;
    private final ContactMessageAssembler contactMessageAssembler;

    public FilterServiceFactory(ComaFilterService comaFilterService, ContactMessageAssembler contactMessageAssembler) {
        this.comaFilterService = comaFilterService;
        this.contactMessageAssembler = contactMessageAssembler;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new FilterService(comaFilterService,contactMessageAssembler);
    }
}
