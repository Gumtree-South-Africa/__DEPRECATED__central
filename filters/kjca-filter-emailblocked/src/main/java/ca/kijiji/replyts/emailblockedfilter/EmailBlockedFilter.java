package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EmailBlockedFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(EmailBlockedFilter.class);

    static final String IS_BLOCKED_KEY = "is-blocked";

    private final int emailBlockedScore;
    private TnsApiClient tnsApiClient;

    EmailBlockedFilter(int emailBlockedScore, TnsApiClient tnsApiClient) {
        this.emailBlockedScore = emailBlockedScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        if (context == null) {
            LOG.error("No context provided");
            return ImmutableList.of();
        }

        Conversation conversation = context.getConversation();
        MessageDirection messageDirection = context.getMessageDirection();

        if (messageDirection != MessageDirection.BUYER_TO_SELLER && messageDirection != MessageDirection.SELLER_TO_BUYER) {
            LOG.warn("Unknown message direction [{}] for conversation [{}]", messageDirection, conversation.getId());
            return ImmutableList.of();
        }

        ImmutableList.Builder<FilterFeedback> feedback = ImmutableList.builder();
        filter(context, conversation.getBuyerId(), "buyer", feedback);
        filter(context, conversation.getSellerId(), "seller", feedback);
        return feedback.build();
    }

    private void filter(MessageProcessingContext context, String email, String owner, ImmutableList.Builder<FilterFeedback> feedback) {
        if (StringUtils.isBlank(email)) {
            LOG.warn("No {} e-mail provided for context {}", owner, context.toString());
        } else if (StringUtils.isNotBlank(email) && checkIfEmailBlockedInLeGrid(email)) {
            feedback.add(new FilterFeedback(owner + " email is blocked", StringUtils.capitalize(owner) + " email is blocked",
                    emailBlockedScore, FilterResultState.DROPPED));
        }
    }

    private boolean checkIfEmailBlockedInLeGrid(String from) {
        Map<String, Boolean> result = tnsApiClient.getJsonAsMap("/replier/email/" + from + "/is-blocked");
        if (result.get(IS_BLOCKED_KEY) == null) {
            LOG.warn("No proper result from TnsApi for e-mail {}, assuming e-mail is not blocked", from);
            return false;
        } else {
            boolean isBlocked = result.get(IS_BLOCKED_KEY);
            LOG.debug("Is email {} blocked? {}", from, isBlocked);
            return isBlocked;
        }
    }
}
