package com.ecg.comaas.core.filter.user;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Matcher;

public class Userfilter implements Filter {

    private final List<PatternEntry> parse;

    public Userfilter(List<PatternEntry> parse) {
        this.parse = parse;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {

        ImmutableList.Builder<FilterFeedback> processingFeedbacks = ImmutableList.<FilterFeedback>builder();

        String buyerMail = messageProcessingContext.getConversation().getBuyerId();
        String sellerMail = messageProcessingContext.getConversation().getSellerId();

        for(PatternEntry pe : parse) {
            checkMailPattern(processingFeedbacks, "buyer", buyerMail, pe);
            checkMailPattern(processingFeedbacks, "seller", sellerMail, pe);
        }

        return processingFeedbacks.build();
    }

    private void checkMailPattern(
            ImmutableList.Builder<FilterFeedback> feedbacks,
            String role,
            String mailStr,
            PatternEntry pe) {

        Matcher mailMatcher = pe.getPattern().matcher(mailStr);

        if(mailMatcher.find()) {
            String description = String.format("%s matches pattern as %s.", mailStr, role);
            feedbacks.add(
                    new FilterFeedback(
                            pe.getPattern().toString(), description, pe.getScore(), FilterResultState.OK));
        }
    }
}
