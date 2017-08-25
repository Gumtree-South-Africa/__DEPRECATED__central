package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
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
        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();
        Conversation conversation = context.getConversation();
        MessageDirection messageDirection = context.getMessageDirection();

        if (messageDirection != MessageDirection.BUYER_TO_SELLER && messageDirection != MessageDirection.SELLER_TO_BUYER) {
            LOG.warn("Unknown message direction [{}] for conversation [{}]", messageDirection, conversation.getId());
            return feedbacks.build();
        }

        boolean buyerEmailBlocked = false;
        boolean sellerEmailBlocked = false;
        try {
            buyerEmailBlocked = checkIfEmailBlockedInLeGrid(conversation.getBuyerId());
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid to check buyer email block. Assuming email not blocked.", e);
        }
        try {
            sellerEmailBlocked = checkIfEmailBlockedInLeGrid(conversation.getSellerId());
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid to check seller email block. Assuming email not blocked.", e);
        }


        if (buyerEmailBlocked) {
            feedbacks.add(new FilterFeedback("buyer email is blocked", "Buyer email is blocked",
                    emailBlockedScore, FilterResultState.DROPPED));
        }
        if (sellerEmailBlocked) {
            feedbacks.add(new FilterFeedback("seller email is blocked", "Seller email is blocked",
                    emailBlockedScore, FilterResultState.DROPPED));
        }

        return feedbacks.build();
    }

    private boolean checkIfEmailBlockedInLeGrid(String from) {
        Map<String, Boolean> result = tnsApiClient.getJsonAsMap("/replier/email/" + from + "/is-blocked");
        boolean isBlocked = result.get(IS_BLOCKED_KEY);
        LOG.trace("Is email {} blocked? {}", from, isBlocked);
        return isBlocked;
    }

}
