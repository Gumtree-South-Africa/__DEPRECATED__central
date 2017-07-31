package com.ebay.columbus.replyts2.quickreply;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by fmaffioletti on 25/07/14.
 */
public class QuickReplyFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(QuickReplyFilter.class);
    protected static final String QUICK_REPLY_HINT = "QUICK_REPLY";
    protected static final String QUICK_REPLY_REASON = "QUICK_REPLY_PLUGIN";

    private List<HeaderEntry> headers;

    public QuickReplyFilter(List<HeaderEntry> headers) {
        this.headers = headers;
    }

    @Override public List<FilterFeedback> filter(MessageProcessingContext context) {
        LOG.debug("Applying QuickReply filter");

        if (!isFirstMessage(context.getConversation())) {
            return Collections.emptyList();
        }

        Map<String, String> customHeaders = context.getMail().getCustomHeaders();
        if (customHeaders.isEmpty()) {
            return Collections.emptyList();
        }

        String matchedHeaders = getMatchedHeaders(customHeaders);

        if (!matchedHeaders.isEmpty()) {
            return Collections.singletonList(new FilterFeedback(QUICK_REPLY_HINT, QUICK_REPLY_REASON + " matched headers: " + matchedHeaders, getFinalScore(customHeaders), FilterResultState.OK));
        }
        return Collections.emptyList();
    }

    private String getMatchedHeaders(Map<String, String> customHeaders) {
        return headers.stream()
                    .filter(header -> customHeaders.containsKey(header.getHeader()))
                    .map(header -> header.getHeader())
                    .collect(Collectors.joining(","));
    }

    private Integer getFinalScore(Map<String, String> customHeaders) {
        return headers.stream()
                    .filter(header -> customHeaders.containsKey(header.getHeader()))
                    .collect(Collectors.summingInt(h -> h.getScore()));
    }

    private boolean isFirstMessage(Conversation conversation) {
        return conversation.getMessages().size() == 1;
    }

}
