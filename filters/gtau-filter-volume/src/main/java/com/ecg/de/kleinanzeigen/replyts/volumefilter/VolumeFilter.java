package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * uses elastic search to query for the number of mails received by the mail's sender. if a quota is violated, a score is assigned.
 * Expects the given list of quotas to be sorted by score descending.
 *
 * @author mhuttar
 */
class VolumeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final String CONTACT_TYPE_CUST_HEADER = "X-Cust-Contact-Type";
    private static final String CONTACT_TYPE_CALL_BACK_REQUEST = "CALL_BACK_REQUEST";

    private final EventStreamProcessor processor;
    private final List<Quota> sortedQuotas;
    private final Set<String> whitelistedEmails;


    VolumeFilter(SharedBrain brain, List<Quota> sortedQuotas, Set<String> whitelistedEmails) {
        this.processor = new EventStreamProcessor(sortedQuotas);
        this.sortedQuotas = sortedQuotas;
        this.whitelistedEmails = whitelistedEmails;
        brain.withProcessor(processor);
    }


    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);
        processor.mailReceivedFrom(senderMailAddress);

        if (isWhitelisted(message, senderMailAddress)) {
            return Collections.emptyList();
        }

        for (Quota q : sortedQuotas) {

            // this mail (that is being processed right now), which is being processed right now, has not been indexed to ES yet
            // therefore, we need to add it to the number of mails in timerange manually.

            long mailsInTimeWindow = processor.count(senderMailAddress, q);


            LOG.debug("Num of mails in {} {}: {}", q.getPerTimeValue(), q.getPerTimeUnit(), mailsInTimeWindow);


            if (mailsInTimeWindow > q.getAllowance()) {
                return Collections.singletonList(new FilterFeedback(
                        q.uihint(),
                        q.describeViolation(mailsInTimeWindow),
                        q.getScore(),
                        FilterResultState.OK));
            }
        }

        return Collections.emptyList();
    }

    private boolean isWhitelisted(Message message, String senderMailAddress) {
        final Optional<String> contactType = Optional.ofNullable(message.getHeaders()).map(m -> m.get(CONTACT_TYPE_CUST_HEADER));

        /**
         * We will whitelist this message if
         * (1) The sender email address is one of the whitelisted emails in the configuration and
         * (2) The message has a header "X-Cust-Contact-Type" with value "CALL_BACK_REQUEST"
         */
        return CollectionUtils.isNotEmpty(this.whitelistedEmails)
                && this.whitelistedEmails.contains(senderMailAddress)
                && contactType.isPresent()
                && CONTACT_TYPE_CALL_BACK_REQUEST.equals(contactType.get());
    }
}
