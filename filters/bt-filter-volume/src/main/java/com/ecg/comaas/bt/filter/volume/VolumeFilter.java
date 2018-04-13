package com.ecg.comaas.bt.filter.volume;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

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
public class VolumeFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final AtomicInteger EVENT_PROCESSOR_COUNTER = new AtomicInteger(0);
    private static final String PROVIDER_NAME_PREFIX = "volumefilter_provider_";

    private final EventStreamProcessor processor;

    private final List<Quota> sortedQuotas;

    private final SharedBrain sharedBrain;

    private final ITopic<String> topic;

    private final boolean ignoreFollowUps;

    private final List<Integer> exceptCategoriesList;

    private final List<Integer> allowedCategoriesList;

    VolumeFilter(String filterName,
                 SharedBrain sharedBrain,
                 List<Quota> sortedQuotas,
                 boolean ignoreFollowUps,
                 List<Integer> exceptCategoriesList,
                 List<Integer> allowedCategoriesList) {
        this.processor = new EventStreamProcessor(format("%s%s_%d", PROVIDER_NAME_PREFIX, filterName, EVENT_PROCESSOR_COUNTER.getAndIncrement()), sortedQuotas);
        this.topic = sharedBrain.createTopic(filterName, processor);
        this.sharedBrain = sharedBrain;
        this.sortedQuotas = sortedQuotas;
        this.ignoreFollowUps = ignoreFollowUps;
        this.exceptCategoriesList = exceptCategoriesList;
        this.allowedCategoriesList = allowedCategoriesList;

        LOG.info("Set up volume filter [{}] with ignoreFollowUps [{}], quotas [{}], exceptCategoriesList [{}] and allowedCategoriesList [{}]", filterName, ignoreFollowUps, sortedQuotas, exceptCategoriesList, allowedCategoriesList);
    }

    private List<FilterFeedback> getRememberedScoreFeedbacks(String senderMailAddress) {
        QuotaViolationRecord violationRecord = null;

        try {
            violationRecord = sharedBrain.getViolationRecordFromMemory(senderMailAddress);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
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
        try {
            sharedBrain.rememberViolation(senderMailAddress, q.getScore(),
              violationDescription + " (triggered at " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS").format(Calendar.getInstance().getTime()) + ")",
              (int) q.getScoreMemoryDurationUnit().toSeconds(q.getScoreMemoryDurationValue()));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Couldn't remember quota violation", e);
        }
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) throws ProcessingTimeExceededException {
        Message message = messageProcessingContext.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);

        String conversation_id = messageProcessingContext.getMail().get().getCustomHeaders().get("conversation_id") != null
          ? messageProcessingContext.getMail().get().getCustomHeaders().get("conversation_id") : null;

        if (conversation_id == null) {
            conversation_id = messageProcessingContext.getConversation().getCustomValues().get("conversation_id") != null
              ? messageProcessingContext.getConversation().getCustomValues().get("conversation_id") : null;
        }

        LOG.debug("Conversation id is {}", conversation_id);

        if (ignoreFollowUps && conversation_id != null) {
            LOG.debug("Ignoring follow-up from [{}]. Msg id: [{}]", senderMailAddress, message.getId());

            return Collections.emptyList();
        }

        Set<String> categorySet = getInMsgCatTree(messageProcessingContext);

        if (isExceptCategory(categorySet)) {
            LOG.debug("Ignoring the message from  [{}] as the category belongs to the configured except categories list", senderMailAddress);

            return Collections.emptyList();
        }

        if (isAllowedCategory(categorySet)) {

            List<FilterFeedback> feedbacksFromRememberedScore = getRememberedScoreFeedbacks(senderMailAddress);

            if (feedbacksFromRememberedScore != null) {
                return feedbacksFromRememberedScore;
            }

            try {
                for (Quota q : sortedQuotas) {
                    long mailsInTimeWindow = processor.count(senderMailAddress, q) + 1;
                    LOG.debug("No. of mails in {} {}: {}", q.getPerTimeValue(), q.getPerTimeUnit(), mailsInTimeWindow);

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
            } finally {
                try {
                    sharedBrain.markSeen(topic, senderMailAddress);
                } catch (Exception ex) {
                    LOG.error("Error occurred during publishing an incoming message to hazelcast", ex);
                }
            }
        }

        return Collections.emptyList();
    }

    private Set<String> getInMsgCatTree(MessageProcessingContext messageProcessingContext) {
        Map<String, String> customHeaders = messageProcessingContext.getMail().get().getCustomHeaders();
        Map<String, String> customValues = messageProcessingContext.getConversation().getCustomValues();

        String category_id = customHeaders.get("categoryid") != null ? customHeaders.get("categoryid") : null;
        if (category_id == null) {
            category_id = customValues.get("categoryid") != null ? customValues.get("categoryid") : null;
        }

        String l1_category_id = customHeaders.get("l1-categoryid") != null ? customHeaders.get("l1-categoryid") : null;
        if (l1_category_id == null) {
            l1_category_id = customValues.get("l1-categoryid") != null ? customValues.get("l1-categoryid") : null;
        }

        String l2_category_id = customHeaders.get("l2-categoryid") != null ? customHeaders.get("l2-categoryid") : null;
        if (l2_category_id == null) {
            l2_category_id = customValues.get("l2-categoryid") != null ? customValues.get("categoryid") : null;
        }

        String l3_category_id = customHeaders.get("l3-categoryid") != null ? customHeaders.get("l3-categoryid") : null;
        if (l3_category_id == null) {
            l3_category_id = customValues.get("l3-categoryid") != null ? customValues.get("categoryid") : null;
        }

        String l4_category_id = customHeaders.get("l4-categoryid") != null ? customHeaders.get("l4-categoryid") : null;
        if (l4_category_id == null) {
            l4_category_id = customValues.get("l4-categoryid") != null ? customValues.get("l4-categoryid") : null;
        }

        Set<String> categorySet = new HashSet<>();

        if (StringUtils.hasText(category_id)) {
            LOG.debug("Category id extracted from incoming msg is  {}", category_id);

            categorySet.add(category_id);
        }

        if (StringUtils.hasText(l1_category_id)) {
            LOG.debug("L1 Category id extracted from incoming msg is {}", l1_category_id);

            categorySet.add(l1_category_id);
        }

        if (StringUtils.hasText(l2_category_id)) {
            LOG.debug("L2 Category id extracted from incoming msg is {}", l2_category_id);

            categorySet.add(l2_category_id);
        }

        if (StringUtils.hasText(l3_category_id)) {
            LOG.debug("L3 Category id extracted from incoming msg is {}", l3_category_id);

            categorySet.add(l3_category_id);
        }

        if (StringUtils.hasText(l4_category_id)) {
            LOG.debug("L4 Category id extracted from incoming msg is {}", l4_category_id);

            categorySet.add(l4_category_id);
        }

        return categorySet;
    }

    private boolean isAllowedCategory(Set<String> categorySet) {
        if (allowedCategoriesList.isEmpty() || categorySet == null) {
            return true;
        }

        return categorySet.stream()
                .filter(c -> allowedCategoriesList.contains(Integer.parseInt(c)))
                .peek(c -> LOG.debug("category id [{}] found in the allowed category list", c))
                .findFirst()
                .isPresent();
    }

    public boolean isExceptCategory(Set<String> categorySet) {
        if (exceptCategoriesList.isEmpty() || categorySet.isEmpty()) {
            return false;
        }

        return categorySet.stream()
                .filter(c -> exceptCategoriesList.contains(Integer.parseInt(c)))
                .peek(c -> LOG.debug("category id [{}] found in the except category list", c))
                .findFirst()
                .isPresent();
    }
}