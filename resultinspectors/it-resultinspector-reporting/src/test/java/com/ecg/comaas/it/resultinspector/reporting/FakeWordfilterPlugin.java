package com.ecg.comaas.it.resultinspector.reporting;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by fmaffioletti on 10/28/15.
 */
public class FakeWordfilterPlugin implements Filter {

    @Override public List<FilterFeedback> filter(MessageProcessingContext context) {
        List<FilterFeedback> fakeFeedbacks = Lists.newArrayList();
        if (context.getMessage().getPlainTextBody().contains("block")) {
            fakeFeedbacks.add(new FilterFeedback("block", "Matched word block", 100,
                            FilterResultState.OK));
        } else {
            fakeFeedbacks.add(new FilterFeedback("test", "is a valid test", 100,
                            FilterResultState.OK));
        }

        return fakeFeedbacks;
    }
}
