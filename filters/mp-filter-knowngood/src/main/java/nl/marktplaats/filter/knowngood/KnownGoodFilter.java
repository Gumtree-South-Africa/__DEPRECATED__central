package nl.marktplaats.filter.knowngood;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KnownGoodFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(KnownGoodFilter.class);

    private String filterName;
    private KnownGoodFilterConfig knownGoodFilterConfig;

    public KnownGoodFilter(String filterName, KnownGoodFilterConfig knownGoodFilterConfig) {
        this.filterName = filterName;
        this.knownGoodFilterConfig = knownGoodFilterConfig;
    }

    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Conversation conv = context.getConversation();
        Mail mail = context.getMail();
        Message message = context.getMessage();

        List<FilterFeedback> reasons = new ArrayList<>();

        String knownGood = getKnownGood(conv.getCustomValues(), message.getMessageDirection());
        if (knownGood != null) {
            LOG.trace("Sender '{}' is known good ({})", mail.getFrom(), knownGood);
            reasons.add(new FilterFeedback(mail.getFrom(), "Sender is known good: " + knownGood, 0, FilterResultState.ACCEPT_AND_TERMINATE));
        }

        return reasons;
    }

    private String getKnownGood(Map<String, String> headers, MessageDirection messageDirection) {
        String knownGoodHeader = MessageDirection.BUYER_TO_SELLER.equals(messageDirection)
                ? knownGoodFilterConfig.getInitiatorGoodHeader()
                : knownGoodFilterConfig.getResponderGoodHeader();

        LOG.trace("HEADER: {}; HEADERS: {}", knownGoodHeader, headers);

        if (headers.containsKey(knownGoodHeader) && headers.get(knownGoodHeader) != null) {
            return headers.get(knownGoodHeader);
        }
        return null;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "@" + hashCode() + ": " + filterName;
    }
}
