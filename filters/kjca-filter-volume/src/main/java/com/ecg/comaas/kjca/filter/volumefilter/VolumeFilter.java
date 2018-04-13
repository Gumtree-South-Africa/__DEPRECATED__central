package com.ecg.comaas.kjca.filter.volumefilter;

import com.ecg.comaas.core.filter.activable.ActivableFilter;
import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Uses Esper to query for the number of mails received by the mail's sender within a quota window.
 * If a quota is violated, a score is assigned.
 * If the quota is configured with "score memory" and a message arrives while we still
 * remember about the previous violation, the score is immediately assigned.
 * Expects to be given a list of quotas sorted by score in a descending order.
 * If multiple quotas are violated, only the one with the highest score is returned.
 * The filter doesn't execute if configured to ignore follow-ups and the message doesn't contain
 * the X-ADID header (indicates initial reply sent from platform).
 */
public class VolumeFilter extends ActivableFilter {
    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final int HAZELCAST_OP_RETRIES = 1;

    private final EventStreamProcessor processor;

    private final List<Quota> sortedQuotas;

    private final SharedBrain sharedBrain;

    private final ITopic<String> topic;

    private final boolean ignoreFollowUps;

    public VolumeFilter(String filterName, SharedBrain sharedBrain, List<Quota> sortedQuotas, boolean ignoreFollowUps, Activation activation, EventStreamProcessor processor) {
        super(activation);

        this.sortedQuotas = sortedQuotas;
        this.ignoreFollowUps = ignoreFollowUps;
        this.sharedBrain = sharedBrain;
        this.topic = sharedBrain.createTopic(filterName, processor);
        this.processor = processor;

        LOG.info("Set up volume filter [{}] with ignoreFollowUps [{}] and quotas [{}]", filterName, ignoreFollowUps, sortedQuotas);
    }

    @Override
    public List<FilterFeedback> doFilter(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);

        if (ignoreFollowUps && messageProcessingContext.getConversation().getMessages().size() != 1) {
            LOG.trace("Ignoring follow-up from [{}]. Msg id: [{}]", senderMailAddress, message.getId());

            return Collections.emptyList();
        }

        sharedBrain.markSeen(topic, processor, senderMailAddress);

        List<FilterFeedback> feedbacksFromRememberedScore = getRememberedScoreFeedbacks(senderMailAddress);

        if (feedbacksFromRememberedScore != null) {
            return feedbacksFromRememberedScore;
        }

        for (Quota q : sortedQuotas) {
            long mailsInTimeWindow = processor.count(senderMailAddress, q);

            LOG.trace("Num of mails in {} {}: {}", q.getPerTimeValue(), q.getPerTimeUnit(), mailsInTimeWindow);

            if (mailsInTimeWindow > q.getAllowance()) {
                String violationDescription = q.describeViolation(mailsInTimeWindow);

                rememberQuotaViolation(senderMailAddress, q, violationDescription);

                return Collections.singletonList(new FilterFeedback(
                  q.uihint(),
                  violationDescription,
                  q.getScore(),
                  FilterResultState.OK));
            }
        }

        return Collections.emptyList();
    }

    private List<FilterFeedback> getRememberedScoreFeedbacks(String senderMailAddress) {
        QuotaViolationRecord violationRecord = null;

        RetryCommand<QuotaViolationRecord> getViolationFromMemoryCmd = new RetryCommand<>(HAZELCAST_OP_RETRIES);

        try {
            violationRecord = getViolationFromMemoryCmd.run(() -> {
                try {
                    return sharedBrain.getViolationRecordFromMemory(senderMailAddress);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOG.warn("Couldn't get score from violation memory. Assuming none.", e);
        }

        if (violationRecord != null) {
            return Collections.singletonList(new FilterFeedback(
              violationRecord.getDescription(),
              "sender previously exceeded quota",
              violationRecord.getScore(),
              FilterResultState.OK));
        }

        return null;
    }

    private void rememberQuotaViolation(String senderMailAddress, Quota q, String violationDescription) {
        RetryCommand<Integer> rememberViolationCmd = new RetryCommand<>(HAZELCAST_OP_RETRIES);

        try {
            rememberViolationCmd.run(() -> {
                try {
                    sharedBrain.rememberViolation(
                      senderMailAddress,
                      q.getScore(),
                      violationDescription + " (triggered at " + LocalDateTime.now() + ")",
                      (int) q.getScoreMemoryDurationUnit().toSeconds(q.getScoreMemoryDurationValue()));
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return 1;
            });
        } catch (Exception e) {
            LOG.warn("Couldn't remember quota violation", e);
        }
    }
}
