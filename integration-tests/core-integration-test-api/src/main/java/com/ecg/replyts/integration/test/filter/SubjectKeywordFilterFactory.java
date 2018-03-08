package com.ecg.replyts.integration.test.filter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static com.ecg.replyts.core.api.model.conversation.FilterResultState.DROPPED;
import static com.ecg.replyts.core.api.model.conversation.FilterResultState.HELD;

/**
 * Filter messages base on keywords "HELD" and "BAD" in the subject.
 */
public class SubjectKeywordFilterFactory implements FilterFactory {

    /**
     * Filter messages base on keywords "HELD" and "DROPPED" in the subject.
     */
    public class SubjectKeywordFilter implements Filter {

        @Override
        public List<FilterFeedback> filter(MessageProcessingContext context) {
            List<FilterFeedback> result = new ArrayList<>();
            if (context.getMail().isPresent()) {
                String subject = context.getMail().get().getSubject();
                appendPFWhenStateInSubject(result, subject, DROPPED);
                appendPFWhenStateInSubject(result, subject, HELD);
            }
            return result;
        }

        private void appendPFWhenStateInSubject(
                List<FilterFeedback> filterFeedbacks,
                String subject, FilterResultState filterResultState
        ) {
            String state = filterResultState.name();
            if (subject.contains(state)) {
                filterFeedbacks.add(new FilterFeedback(
                        state + " in subject",
                        state + " in subject",
                        50,
                        filterResultState
                ));
            }
        }
    }


    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        return new SubjectKeywordFilter();
    }

}
