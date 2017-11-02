package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.ecg.gumtree.replyts2.common.message.MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static java.util.Optional.ofNullable;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class ConversationResponseDiff {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final long ONE_MINUTE_IN_MILLIS = 60000;

    private final Counter diffCounter = newCounter("diff.conversationResponseDiff.counter");
    private final DiffReporter reporter;
    private final boolean checkUnreadCounts;

    @Autowired
    public ConversationResponseDiff(DiffReporter reporter,
                                    @Value("${messagebox.diff.checkUnreadCounts:true}") boolean checkUnreadCounts) {
        this.reporter = reporter;
        this.checkUnreadCounts = checkUnreadCounts;
    }

    public void diff(String userId, String conversationId, Optional<ConversationThread> newValueOpt, Optional<PostBoxSingleConversationThreadResponse> oldValueOpt) {
        String params = new StringJoiner(",").add(userId).add(conversationId).toString();

        if (newValueOpt.isPresent() && oldValueOpt.isPresent()) {
            diff(userId, params, newValueOpt.get(), oldValueOpt.get());
        } else {
            log(params, "conversation responses are missing",
                    newValueOpt.map(value -> "no").orElse("yes"),
                    oldValueOpt.map(value -> "no").orElse("yes"),
                    false);
        }
    }

    private void diff(String userId, String params, ConversationThread newValue, PostBoxSingleConversationThreadResponse oldValue) {
        boolean useNewLogger = newValue.getMetadata().getCreationDate() != null;
        BuyerSellerInfo bsInfo = new BuyerSellerInfo.BuyerSellerInfoBuilder(newValue.getParticipants()).build();

        if (!newValue.getId().equals(oldValue.getId())) {
            log(params, "id", newValue.getId(), oldValue.getId(), useNewLogger);
        }

        ConversationRole newConvRole = ConversationRoleUtil.getConversationRole(userId, newValue.getParticipants());
        if (newConvRole != oldValue.getRole()) {
            log(params, "role", newConvRole.name(), oldValue.getRole().name(), useNewLogger);
        }

        if (!bsInfo.getBuyerEmail().equals(oldValue.getBuyerEmail())) {
            log(params, "buyerEmail", bsInfo.getBuyerEmail(), oldValue.getBuyerEmail(), useNewLogger);
        }

        if (!bsInfo.getSellerEmail().equals(oldValue.getSellerEmail())) {
            log(params, "sellerEmail", bsInfo.getSellerEmail(), oldValue.getSellerEmail(), useNewLogger);
        }

        if (!ofNullable(bsInfo.getBuyerName()).orElse("").equals(oldValue.getBuyerName())) {
            log(params, "buyerName", bsInfo.getBuyerName(), oldValue.getBuyerName(), useNewLogger);
        }

        if (!ofNullable(bsInfo.getSellerName()).orElse("").equals(oldValue.getSellerName())) {
            log(params, "sellerName", bsInfo.getSellerName(), oldValue.getSellerName(), useNewLogger);
        }

        if (!newValue.getAdId().equals(oldValue.getAdId())) {
            log(params, "adId", newValue.getAdId(), oldValue.getAdId(), useNewLogger);
        }

        if (checkUnreadCounts) {
            if (newValue.getNumUnreadMessages(userId) != oldValue.getNumUnread()) {
                log(params, "numUnread", Long.toString(newValue.getNumUnreadMessages(userId)), Long.toString(oldValue.getNumUnread()), useNewLogger);
            }
        }

        List<MessageResponse> newMessages = newValue.getMessages().stream()
                .map(message -> toMessageResponse(message, userId, newValue.getParticipants()))
                .collect(Collectors.toList());
        List<MessageResponse> oldMessages = oldValue.getMessages();

        if (newMessages.size() != oldMessages.size()) {
            log(params, "messages size", Integer.toString(newMessages.size()), Integer.toString(oldMessages.size()), useNewLogger);
        } else {
            for (int i = 0; i < newMessages.size(); i++) {
                MessageResponse newMsgResponse = newMessages.get(i);
                MessageResponse oldMsgResponse = oldMessages.get(i);
                String logMsgPrefix = "";

                if (newMsgResponse.getBoundness() != oldMsgResponse.getBoundness()) {
                    log(params, logMsgPrefix + ".boundness", newMsgResponse.getBoundness().name(), oldMsgResponse.getBoundness().name(), useNewLogger);
                }
                if (!newMsgResponse.getTextShort().equals(oldMsgResponse.getTextShort())) {
                    log(params, logMsgPrefix + ".textShort", newMsgResponse.getTextShort(), oldMsgResponse.getTextShort(), useNewLogger);
                }

                // comparing with a margin of 1 minute apart, due to different timestamps inserted in old and new model
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
                long oldReceivedDateMillis, newReceivedDateMillis;
                try {
                    oldReceivedDateMillis = format.parse(newMsgResponse.getReceivedDate()).getTime();
                    newReceivedDateMillis = format.parse(oldMsgResponse.getReceivedDate()).getTime();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                if (Math.abs(oldReceivedDateMillis - newReceivedDateMillis) > ONE_MINUTE_IN_MILLIS) {
                    log(params, logMsgPrefix + ".receivedDate", newMsgResponse.getReceivedDate(), oldMsgResponse.getReceivedDate(), useNewLogger);
                }
            }
        }
    }

    public MessageResponse toMessageResponse(Message message, String projectionOwnerUserId, List<Participant> participants) {
        Participant participant1 = participants.get(0);
        Participant participant2 = participants.get(1);
        String senderEmail = participant1.getUserId().equals(message.getSenderUserId()) ?
                participant1.getEmail() : participant2.getEmail();
        String buyerEmail = participant1.getUserId().equals(message.getSenderUserId()) ?
                participant2.getEmail() : participant1.getEmail();

        MailTypeRts boundness = message.getSenderUserId().equals(projectionOwnerUserId) ? MailTypeRts.OUTBOUND : MailTypeRts.INBOUND;

        return new MessageResponse(
                toFormattedTimeISO8601ExplicitTimezoneOffset(message.getReceivedDate()),
                null,
                boundness,
                message.getText(),
                Optional.empty(),
                Collections.emptyList(),
                senderEmail,
                buyerEmail);
    }

    private void log(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        diffCounter.inc();
        reporter.report(
                String.format("conversationResponseDiff(%s) - %s - new: '%s' vs old: '%s'", params, fieldName, newValue, oldValue),
                useNewLogger);
    }
}