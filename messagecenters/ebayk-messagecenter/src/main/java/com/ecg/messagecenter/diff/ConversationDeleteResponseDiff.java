package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class ConversationDeleteResponseDiff {

    private static final Pattern REMOVE_DOUBLE_WHITESPACES = Pattern.compile("\\s+");
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final long ONE_MINUTE_IN_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Counter diffCounter = newCounter("diff.conversationResponseDiff.counter");
    private final int maxChars;

    @Autowired
    public ConversationDeleteResponseDiff(@Value("${replyts.maxPreviewMessageCharacters:250}") int maxChars) {
        this.maxChars = maxChars;
    }

    public void diff(String userId, String conversationId, Optional<ConversationThread> newValueOpt, Optional<ConversationRts> oldValueOpt) {
        String params = String.join(",", userId, conversationId);

        if (newValueOpt.isPresent() && oldValueOpt.isPresent()) {
            diff(userId, params, newValueOpt.get(), oldValueOpt.get());
        } else {
            log(params, "conversation responses are missing",
                    newValueOpt.map(value -> "no").orElse("yes"),
                    oldValueOpt.map(value -> "no").orElse("yes"),
                    false);
        }
    }

    private void diff(String userId, String params, ConversationThread newValue, ConversationRts oldValue) {
        boolean useNewLogger = newValue.getMetadata().getCreationDate() != null;
        BuyerSellerInfo bsInfo = new BuyerSellerInfo.BuyerSellerInfoBuilder(newValue.getParticipants()).build();

        if (!newValue.getId().equals(oldValue.getId())) {
            log(params, "id", newValue.getId(), oldValue.getId(), useNewLogger);
        }

        ConversationRole newConvRole = ConversationDiffUtil.getConversationRole(userId, newValue.getParticipants());
        ConversationRole oldConvRole = ConversationDiffUtil.getConversationRole(userId, oldValue.getBuyer(), oldValue.getSeller());
        if (newConvRole != oldConvRole) {
            log(params, "role", newConvRole.name(), oldConvRole.name(), useNewLogger);
        }

        if (!bsInfo.getBuyerEmail().equals(oldValue.getBuyer())) {
            log(params, "buyerEmail", bsInfo.getBuyerEmail(), oldValue.getBuyer(), useNewLogger);
        }

        if (!bsInfo.getSellerEmail().equals(oldValue.getSeller())) {
            log(params, "sellerEmail", bsInfo.getSellerEmail(), oldValue.getSeller(), useNewLogger);
        }

        if (!newValue.getAdId().equals(oldValue.getAdId())) {
            log(params, "adId", newValue.getAdId(), oldValue.getAdId(), useNewLogger);
        }

        List<MessageResponse> newMessages = newValue.getMessages().stream()
                .map(message -> ConversationDiffUtil.toMessageResponse(message, userId, newValue.getParticipants()))
                .collect(Collectors.toList());
        List<MessageRts> oldMessages = oldValue.getMessages();

        if (newMessages.size() != oldMessages.size()) {
            log(params, "messages size", Integer.toString(newMessages.size()), Integer.toString(oldMessages.size()), useNewLogger);
        } else {
            for (int i = 0; i < newMessages.size(); i++) {
                MessageResponse newMsgResponse = newMessages.get(i);
                MessageRts oldMsgResponse = oldMessages.get(i);
                String logMsgPrefix = "";

                MailTypeRts oldBoundness = oldMsgResponse.getConversation().getSeller().equals(userId) ? MailTypeRts.OUTBOUND : MailTypeRts.INBOUND;
                if (newMsgResponse.getBoundness() != oldBoundness) {
                    log(params, logMsgPrefix + ".boundness", newMsgResponse.getBoundness().name(), oldBoundness.name(), useNewLogger);
                }

                String oldFullText = oldMsgResponse.getText();
                String oldTrimmed = REMOVE_DOUBLE_WHITESPACES.matcher(oldFullText).replaceAll(" ");
                String oldShortMsg = MessageCenterUtils.truncateText(oldTrimmed, maxChars);
                if (!newMsgResponse.getTextShort().equals(oldShortMsg)) {
                    log(params, logMsgPrefix + ".textShort", newMsgResponse.getTextShort(), oldShortMsg, useNewLogger);
                }


                // comparing with a margin of 1 minute apart, due to different timestamps inserted in old and new model
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                long oldReceivedDateMillis, newReceivedDateMillis;
                try {
                    oldReceivedDateMillis = format.parse(newMsgResponse.getReceivedDate()).getTime();
                    newReceivedDateMillis = oldMsgResponse.getReceivedDate().getTime();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

                if (Math.abs(oldReceivedDateMillis - newReceivedDateMillis) > ONE_MINUTE_IN_MILLIS) {
                    log(params, logMsgPrefix + ".receivedDate", newMsgResponse.getReceivedDate(), oldMsgResponse.getReceivedDate().toString(), useNewLogger);
                }
            }
        }
    }

    private void log(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        diffCounter.inc();
        DiffReporter.report(
                String.format("conversationResponseDiff(%s) - %s - new: '%s' vs old: '%s'", params, fieldName, newValue, oldValue),
                useNewLogger);
    }
}