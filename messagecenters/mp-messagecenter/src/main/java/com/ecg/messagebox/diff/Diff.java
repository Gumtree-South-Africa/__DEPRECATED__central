package com.ecg.messagebox.diff;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

/**
 * Diff tool that checks and logs differences between old model and new model responses.
 */
@Component
public class Diff {

    private static final Logger LOGGER = LoggerFactory.getLogger("diffLogger");

    private final Timer pbRespDiffTimer = newTimer("diff.postBoxResponseDiff.timer");
    private final Timer convRespDiffTimer = newTimer("diff.conversationResponseDiff.timer");
    private final Timer unreadCountsDiffTimer = newTimer("diff.postBoxUnreadCountsDiff.timer");

    private final Counter pbRespDiffCounter = newCounter("diff.postBoxResponseDiff.counter");
    private final Counter convRespDiffCounter = newCounter("diff.conversationResponseDiff.counter");
    private final Counter unreadCountsDiffCounter = newCounter("diff.postBoxUnreadCountsDiff.counter");

    private final boolean checkUnreadCounts;

    @Autowired
    public Diff(@Value("${messagebox.diff.checkUnreadCounts:true}") boolean checkUnreadCounts) {
        this.checkUnreadCounts = checkUnreadCounts;
    }

    public void postBoxResponseDiff(String userId,
                                    CompletableFuture<PostBoxResponse> newPbRespFuture,
                                    CompletableFuture<PostBoxResponse> oldPbRespFuture) {

        try (Timer.Context ignored = pbRespDiffTimer.time()) {

            PostBoxResponse newValue, oldValue;
            try {
                newValue = newPbRespFuture.join();
                oldValue = oldPbRespFuture.join();
            } catch (Throwable ignore) {
                // no point to continue with diffing
                // exception was already logged in main flow - see PostBoxServiceDelegator
                return;
            }

            if (checkUnreadCounts) {
                if (newValue.getNumUnread() != oldValue.getNumUnread()) {
                    logDiffForPbResp(userId, "numUnread",
                            Integer.toString(newValue.getNumUnread()),
                            Integer.toString(oldValue.getNumUnread()));
                }
            }

            if (!newValue.get_meta().equals(oldValue.get_meta())) {
                logDiffForPbResp(userId, "_meta", newValue.get_meta().toString(), oldValue.get_meta().toString());
            }

            List<PostBoxListItemResponse> newConversations = newValue.getConversations();
            List<PostBoxListItemResponse> oldConversations = oldValue.getConversations();

            if (newConversations.size() != oldConversations.size()) {
                logDiffForPbResp(userId, "conversations size",
                        Integer.toString(newConversations.size()),
                        Integer.toString(oldConversations.size()));
            } else {
                for (int i = 0; i < newConversations.size(); i++) {
                    PostBoxListItemResponse newConv = newConversations.get(i);
                    PostBoxListItemResponse oldConv = oldConversations.get(i);
                    String logConvPrefix = "conversations[" + newConv.getId() + "](" + i + ")";

                    if (!newConv.getId().equals(oldConv.getId())) {
                        logDiffForPbResp(userId, logConvPrefix + ".id", newConv.getId(), oldConv.getId());
                    }
                    if (!newConv.getBuyerName().equals(oldConv.getBuyerName())) {
                        logDiffForPbResp(userId, logConvPrefix + ".buyerName", newConv.getBuyerName(), oldConv.getBuyerName());
                    }
                    if (!newConv.getSellerName().equals(oldConv.getSellerName())) {
                        logDiffForPbResp(userId, logConvPrefix + ".sellerName", newConv.getSellerName(), oldConv.getSellerName());
                    }
                    if (!newConv.getUserIdBuyer().equals(oldConv.getUserIdBuyer())) {
                        logDiffForPbResp(userId, logConvPrefix + ".userIdBuyer", newConv.getUserIdBuyer().toString(), oldConv.getUserIdBuyer().toString());
                    }
                    if (!newConv.getUserIdSeller().equals(oldConv.getUserIdSeller())) {
                        logDiffForPbResp(userId, logConvPrefix + ".userIdSeller", newConv.getUserIdSeller().toString(), oldConv.getUserIdSeller().toString());
                    }
                    if (!newConv.getAdId().equals(oldConv.getAdId())) {
                        logDiffForPbResp(userId, logConvPrefix + ".adId", newConv.getAdId(), oldConv.getAdId());
                    }
                    if (newConv.getRole() != oldConv.getRole()) {
                        logDiffForPbResp(userId, logConvPrefix + ".role", newConv.getRole().name(), oldConv.getRole().name());
                    }
                    if (checkUnreadCounts) {
                        if (newConv.getNumUnreadMessages() != oldConv.getNumUnreadMessages()) {
                            logDiffForPbResp(userId, logConvPrefix + ".numUnreadMessages", Integer.toString(newConv.getNumUnreadMessages()), Integer.toString(oldConv.getNumUnreadMessages()));
                        }
                    }
                    // conversation's latest message
                    if (newConv.getBoundness() != oldConv.getBoundness()) {
                        logDiffForPbResp(userId, logConvPrefix + ".boundness", newConv.getBoundness().name(), oldConv.getBoundness().name());
                    }
                    if (!newConv.getTextShortTrimmed().equals(oldConv.getTextShortTrimmed())) {
                        logDiffForPbResp(userId, logConvPrefix + ".textShortTrimmed", newConv.getTextShortTrimmed(), oldConv.getTextShortTrimmed());
                    }
                    // comparing at minute level only, due to different timestamps inserted in old and new model, so they are a couple of ms apart
                    if (!newConv.getReceivedDate().substring(0, 17).equals(oldConv.getReceivedDate().substring(0, 17))) {
                        logDiffForPbResp(userId, logConvPrefix + ".receivedDate", newConv.getReceivedDate(), oldConv.getReceivedDate());
                    }
                }
            }
        }
    }

    public void conversationResponseDiff(String userId, String conversationId,
                                         CompletableFuture<Optional<ConversationResponse>> newConvRespFuture,
                                         CompletableFuture<Optional<ConversationResponse>> oldConvRespFuture) {

        try (Timer.Context ignored = convRespDiffTimer.time()) {

            Optional<ConversationResponse> newValue, oldValue;
            try {
                newValue = newConvRespFuture.join();
                oldValue = oldConvRespFuture.join();
            } catch (Throwable ignore) {
                // no point to continue with diffing
                // exception was already logged in main flow - see PostBoxServiceDelegator
                return;
            }

            String params = new StringJoiner(",").add(userId).add(conversationId).toString();

            ConversationResponse oldConvResp = oldValue.get(), newConvResp = newValue.get();

            if (!newConvResp.getId().equals(oldConvResp.getId())) {
                logDiffForConvResp(params, "id", newConvResp.getId(), oldConvResp.getId());
            }

            if (newConvResp.getRole() != oldConvResp.getRole()) {
                logDiffForConvResp(params, "role", newConvResp.getRole().name(), oldConvResp.getRole().name());
            }

            if (!newConvResp.getBuyerEmail().equals(oldConvResp.getBuyerEmail())) {
                logDiffForConvResp(params, "buyerEmail", newConvResp.getBuyerEmail(), oldConvResp.getBuyerEmail());
            }

            if (!newConvResp.getSellerEmail().equals(oldConvResp.getSellerEmail())) {
                logDiffForConvResp(params, "sellerEmail", newConvResp.getSellerEmail(), oldConvResp.getSellerEmail());
            }

            if (!newConvResp.getBuyerName().equals(oldConvResp.getBuyerName())) {
                logDiffForConvResp(params, "buyerName", newConvResp.getBuyerName(), oldConvResp.getBuyerName());
            }

            if (!newConvResp.getSellerName().equals(oldConvResp.getSellerName())) {
                logDiffForConvResp(params, "sellerName", newConvResp.getSellerName(), oldConvResp.getSellerName());
            }

            if (!newConvResp.getSellerName().equals(oldConvResp.getSellerName())) {
                logDiffForConvResp(params, "sellerName", newConvResp.getSellerName(), oldConvResp.getSellerName());
            }

            if (!newConvResp.getUserIdBuyer().equals(oldConvResp.getUserIdBuyer())) {
                logDiffForConvResp(params, "userIdBuyer", Long.toString(newConvResp.getUserIdBuyer()), Long.toString(oldConvResp.getUserIdBuyer()));
            }

            if (!newConvResp.getUserIdSeller().equals(oldConvResp.getUserIdSeller())) {
                logDiffForConvResp(params, "userIdSeller", Long.toString(newConvResp.getUserIdSeller()), Long.toString(oldConvResp.getUserIdSeller()));
            }

            if (!newConvResp.getAdId().equals(oldConvResp.getAdId())) {
                logDiffForConvResp(params, "adId", newConvResp.getAdId(), oldConvResp.getAdId());
            }

            if (!newConvResp.getSubject().equals(oldConvResp.getSubject())) {
                logDiffForConvResp(params, "subject", newConvResp.getSubject(), oldConvResp.getSubject());
            }

            if (checkUnreadCounts) {
                if (newConvResp.getNumUnread() != oldConvResp.getNumUnread()) {
                    logDiffForConvResp(params, "numUnread", Long.toString(newConvResp.getNumUnread()), Long.toString(oldConvResp.getNumUnread()));
                }
            }

            List<MessageResponse> newMessages = newConvResp.getMessages();
            List<MessageResponse> oldMessages = oldConvResp.getMessages();

            if (newMessages.size() != oldMessages.size()) {
                logDiffForConvResp(params, "messages size", Integer.toString(newMessages.size()), Integer.toString(oldMessages.size()));
            } else {
                for (int i = 0; i < newMessages.size(); i++) {
                    MessageResponse newMsgResp = newMessages.get(i);
                    MessageResponse oldMsgResp = oldMessages.get(i);
                    String logMsgPrefix = "messages[" + newMsgResp.getMessageId().or("") + "](" + i + ")";

                    if (!newMsgResp.getSenderEmail().equals(oldMsgResp.getSenderEmail())) {
                        logDiffForConvResp(params, logMsgPrefix + ".senderEmail", newMsgResp.getSenderEmail(), oldMsgResp.getSenderEmail());
                    }
                    if (newMsgResp.getBoundness() != oldMsgResp.getBoundness()) {
                        logDiffForConvResp(params, logMsgPrefix + ".boundness", newMsgResp.getBoundness().name(), oldMsgResp.getBoundness().name());
                    }
                    if (!newMsgResp.getTextShort().equals(oldMsgResp.getTextShort())) {
                        logDiffForConvResp(params, logMsgPrefix + ".textShort", newMsgResp.getTextShort(), oldMsgResp.getTextShort());
                    }
                    // comparing at minute level only, due to different timestamps inserted in old and new model, so they are a couple of ms apart
                    if (!newMsgResp.getReceivedDate().substring(0, 17).equals(oldMsgResp.getReceivedDate().substring(0, 17))) {
                        logDiffForConvResp(params, logMsgPrefix + ".receivedDate", newMsgResp.getReceivedDate(), oldMsgResp.getReceivedDate());
                    }
                }
            }
        }
    }

    public void postBoxUnreadCountsDiff(String userId,
                                        CompletableFuture<PostBoxUnreadCounts> newUnreadCountsFuture,
                                        CompletableFuture<PostBoxUnreadCounts> oldUnreadCountsFuture) {
        if (checkUnreadCounts) {
            try (Timer.Context ignored = unreadCountsDiffTimer.time()) {

                PostBoxUnreadCounts newValue, oldValue;
                try {
                    newValue = newUnreadCountsFuture.join();
                    oldValue = oldUnreadCountsFuture.join();
                } catch (Throwable ignore) {
                    // no point to continue with diffing
                    // exception was already logged in main flow - see PostBoxServiceDelegator
                    return;
                }

                if (newValue.getNumUnreadConversations() != oldValue.getNumUnreadConversations()) {
                    logDiffForUnreadCounts(userId, "numUnreadConversations",
                            Integer.toString(newValue.getNumUnreadConversations()),
                            Integer.toString(oldValue.getNumUnreadConversations()));
                }

                if (newValue.getNumUnreadMessages() != oldValue.getNumUnreadMessages()) {
                    logDiffForUnreadCounts(userId, "numUnreadMessages",
                            Integer.toString(newValue.getNumUnreadMessages()),
                            Integer.toString(oldValue.getNumUnreadMessages()));
                }
            }
        }
    }

    private void logDiffForPbResp(String params, String fieldName, String newValue, String oldValue) {
        pbRespDiffCounter.inc();
        logDiff("postBoxResponseDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiffForConvResp(String params, String fieldName, String newValue, String oldValue) {
        convRespDiffCounter.inc();
        logDiff("conversationResponseDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiffForUnreadCounts(String params, String fieldName, String newValue, String oldValue) {
        unreadCountsDiffCounter.inc();
        logDiff("unreadCountsDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiff(String methodName, String params, String fieldName, String newValue, String oldValue) {
        LOGGER.error(String.format("%s(%s) - %s - new: '%s' vs old: '%s'", methodName, params, fieldName, newValue, oldValue));
    }
}