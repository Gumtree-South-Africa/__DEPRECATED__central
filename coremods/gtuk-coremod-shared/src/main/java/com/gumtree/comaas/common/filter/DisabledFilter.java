package com.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class DisabledFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisabledFilter.class);

    public DisabledFilter(Class clazz) {
        LOGGER.info("Filter {} is configured but disabled.", clazz.getName());
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        return Collections.emptyList();
    }
}
