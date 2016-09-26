package com.ecg.messagebox.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static java.util.Optional.ofNullable;

@Component
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

    public void diff(String userId, String conversationId, Optional<ConversationResponse> newValueOpt, Optional<ConversationResponse> oldValueOpt) {
        String params = new StringJoiner(",").add(userId).add(conversationId).toString();

        if (newValueOpt.isPresent() && oldValueOpt.isPresent()) {
            diff(params, newValueOpt.get(), oldValueOpt.get());
        } else {
            log(params, "conversation responses are missing", newValueOpt.map(value -> "no").orElse("yes"), oldValueOpt.map(value -> "no").orElse("yes"), false);
        }
    }

    private void diff(String params, ConversationResponse newValue, ConversationResponse oldValue) {
        boolean useNewLogger = newValue.getCreationDate() != null;

        if (!newValue.getId().equals(oldValue.getId())) {
            log(params, "id", newValue.getId(), oldValue.getId(), useNewLogger);
        }

        if (newValue.getRole() != oldValue.getRole()) {
            log(params, "role", newValue.getRole().name(), oldValue.getRole().name(), useNewLogger);
        }

        if (!newValue.getBuyerEmail().equals(oldValue.getBuyerEmail())) {
            log(params, "buyerEmail", newValue.getBuyerEmail(), oldValue.getBuyerEmail(), useNewLogger);
        }

        if (!newValue.getSellerEmail().equals(oldValue.getSellerEmail())) {
            log(params, "sellerEmail", newValue.getSellerEmail(), oldValue.getSellerEmail(), useNewLogger);
        }

        if (!ofNullable(newValue.getBuyerName()).orElse("").equals(oldValue.getBuyerName())) {
            log(params, "buyerName", newValue.getBuyerName(), oldValue.getBuyerName(), useNewLogger);
        }

        if (!ofNullable(newValue.getSellerName()).orElse("").equals(oldValue.getSellerName())) {
            log(params, "sellerName", newValue.getSellerName(), oldValue.getSellerName(), useNewLogger);
        }

        if (!newValue.getUserIdBuyer().equals(oldValue.getUserIdBuyer())) {
            log(params, "userIdBuyer", Long.toString(newValue.getUserIdBuyer()), Long.toString(oldValue.getUserIdBuyer()), useNewLogger);
        }

        if (!newValue.getUserIdSeller().equals(oldValue.getUserIdSeller())) {
            log(params, "userIdSeller", Long.toString(newValue.getUserIdSeller()), Long.toString(oldValue.getUserIdSeller()), useNewLogger);
        }

        if (!newValue.getAdId().equals(oldValue.getAdId())) {
            log(params, "adId", newValue.getAdId(), oldValue.getAdId(), useNewLogger);
        }

        if (!newValue.getSubject().equals(oldValue.getSubject())) {
            log(params, "subject", newValue.getSubject(), oldValue.getSubject(), useNewLogger);
        }

        if (checkUnreadCounts) {
            if (newValue.getNumUnread() != oldValue.getNumUnread()) {
                log(params, "numUnread", Long.toString(newValue.getNumUnread()), Long.toString(oldValue.getNumUnread()), useNewLogger);
            }
        }

        List<MessageResponse> newMessages = newValue.getMessages();
        List<MessageResponse> oldMessages = oldValue.getMessages();

        if (newMessages.size() != oldMessages.size()) {
            log(params, "messages size", Integer.toString(newMessages.size()), Integer.toString(oldMessages.size()), useNewLogger);
        } else {
            for (int i = 0; i < newMessages.size(); i++) {
                MessageResponse newMsgResponse = newMessages.get(i);
                MessageResponse oldMsgResponse = oldMessages.get(i);
                String logMsgPrefix = newMsgResponse.getMessageType() + " - messages[" + newMsgResponse.getMessageId().or("") + "](" + i + ")";

                // caters for the ABQ bug in the old model, where buyer id and seller id were the same
                // this bug affects the way we calculate the senderEmail in new model
                if (!"ABQ".equalsIgnoreCase(newMsgResponse.getMessageType()) || !oldValue.getUserIdSeller().equals(oldValue.getUserIdBuyer())) {
                    if (!newMsgResponse.getSenderEmail().equals(oldMsgResponse.getSenderEmail())) {
                        log(params, logMsgPrefix + ".senderEmail", newMsgResponse.getSenderEmail(), oldMsgResponse.getSenderEmail(), useNewLogger);
                    }
                }

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

    private void log(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        diffCounter.inc();
        reporter.report(
                String.format("conversationResponseDiff(%s) - %s - new: '%s' vs old: '%s'", params, fieldName, newValue, oldValue),
                useNewLogger
        );
    }
}