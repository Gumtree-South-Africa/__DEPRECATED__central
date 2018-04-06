package com.ecg.de.mobile.replyts.comafilterservice;

import com.ecg.de.mobile.replyts.comafilterservice.filters.ComaFilterService;
import com.ecg.de.mobile.replyts.comafilterservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

class FilterServiceFactory implements FilterFactory {

    public static final String IDENTIFIER = "com.ecg.de.mobile.replyts.comafilterservice.FilterServiceFactory";

    private final ComaFilterService comaFilterService;
    private final ContactMessageAssembler contactMessageAssembler;

    public FilterServiceFactory(ComaFilterService comaFilterService, ContactMessageAssembler contactMessageAssembler) {
        this.comaFilterService = comaFilterService;
        this.contactMessageAssembler = contactMessageAssembler;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new FilterService(comaFilterService, contactMessageAssembler);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
