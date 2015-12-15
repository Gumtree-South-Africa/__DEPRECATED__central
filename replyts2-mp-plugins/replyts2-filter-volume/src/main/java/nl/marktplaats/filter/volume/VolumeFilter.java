package nl.marktplaats.filter.volume;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;
import nl.marktplaats.filter.volume.persistence.VolumeFilterEventRepository;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;

public class VolumeFilter implements Filter {

    private static final String BID_FLOW_TYPE = "PLACED_BID";

    private VolumeFilterEventRepository volumeFilterEventRepository;

    /**
     * List of rules sorted by their descending scores. All rules need to be checked (due to moving timeframes), but
     * only the highest score will be taken into account. Therefore execution can stop after the first rule violation,
     * as all following rules will produce a smaller score
     */
    private List<VolumeRule> sortedRules;

    public VolumeFilter(VolumeFilterConfiguration volumeFilterConfiguration, VolumeFilterEventRepository volumeFilterEventRepository) {
        this.sortedRules = new RuleSorter().orderRules(volumeFilterConfiguration);
        this.volumeFilterEventRepository = volumeFilterEventRepository;
    }

    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        Conversation conv = context.getConversation();
        Mail mail = context.getMail();
        Message message = context.getMessage();

        if (isFirstMailInConversation(conv, message) && !isBid(conv)) {
            recordMessage(message, conv);

            String userId = extractFrom(message, conv);
            for (VolumeRule rule : sortedRules) {
                int sentActually = volumeFilterEventRepository.count(userId, (int) rule.getTimeUnit().toSeconds(rule.getTimeSpan()));
                if (rule.getMaxCount() < sentActually) {
                    String uiHint = toUiHint(rule, mail.getFrom());
                    String description = toDescription(rule, mail.getFrom());
                    return Collections.singletonList(new FilterFeedback(uiHint, description, rule.getScore(), FilterResultState.OK));
                }
            }
        }
        return Collections.emptyList();
    }

    private static String toUiHint(VolumeRule vr, String userId) {
        //TODO: convert to string concat
        return String.format("%s>%s mails/%s %s +%s", userId, vr.getMaxCount(), vr.getTimeSpan(), vr.getTimeUnit(), vr.getScore());
    }

    private static String toDescription(VolumeRule vr, String userId) {
        //TODO: convert to string concat
        return String.format("%s sent more than %s mails the last %s %s", userId, vr.getMaxCount(), vr.getTimeSpan(), vr.getTimeUnit());
    }

    private boolean isBid(Conversation conv) {
        return BID_FLOW_TYPE.equals(conv.getCustomValues().get("flowtype"));
    }

    private boolean isFirstMailInConversation(Conversation conversation, Message message) {
        return conversation.getMessages().get(0).getId().equals(message.getId());
    }

    private void recordMessage(Message message, Conversation conversation) {
        String userId = extractFrom(message, conversation);
        Long longestTimeSpan =
                sortedRules
                .stream()
                .map(rule -> rule.getTimeUnit().toSeconds(rule.getTimeSpan()))
                .max(Long::compare)
                .get();
        volumeFilterEventRepository.record(userId, longestTimeSpan.intValue());
    }

    private String extractFrom(Message message, Conversation conv) {
        return conv.getUserId(message.getMessageDirection().getFromRole());
    }
}
