package com.ecg.messagebox.diff;

import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

/**
 * Diff tool that checks and logs differences between old model and new model responses.
 */
@Component
public class Diff {

    private static final Logger LOGGER = LoggerFactory.getLogger("diffLogger");

    public void postBoxResponseDiff(String userId,
                                    CompletableFuture<PostBoxResponse> newPbRespFuture,
                                    CompletableFuture<PostBoxResponse> oldPbRespFuture) {

        PostBoxResponse newValue, oldValue;
        try {
            newValue = newPbRespFuture.join();
            oldValue = oldPbRespFuture.join();
        } catch (Throwable ignore) {
            // no point to continue with diffing
            // exception was already logged in main flow - see PostBoxServiceDelegator
            return;
        }

        if (newValue.getNumUnread() != oldValue.getNumUnread()) {
            logDiffForPbResp(userId, "numUnread",
                    Integer.toString(newValue.getNumUnread()),
                    Integer.toString(oldValue.getNumUnread()));
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

                if (!newConv.equals(oldConv)) {
                    logDiffForPbResp(userId, "conversations(" + i + ")", newConv.toString(), oldConv.toString());
                }
            }
        }
    }

    public void conversationResponseDiff(String userId, String conversationId,
                                         CompletableFuture<Optional<ConversationResponse>> newConvRespFuture,
                                         CompletableFuture<Optional<ConversationResponse>> oldConvRespFuture) {

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

        if (newConvResp.getNumUnread() != oldConvResp.getNumUnread()) {
            logDiffForConvResp(params, "numUnread", Long.toString(newConvResp.getNumUnread()), Long.toString(oldConvResp.getNumUnread()));
        }

        List<MessageResponse> newMessages = newConvResp.getMessages();
        List<MessageResponse> oldMessages = oldConvResp.getMessages();

        if (newMessages.size() != oldMessages.size()) {
            logDiffForConvResp(params, "messages size", Integer.toString(newMessages.size()), Integer.toString(oldMessages.size()));
        } else {
            for (int i = 0; i < newMessages.size(); i++) {
                MessageResponse newMsgResp = newMessages.get(i);
                MessageResponse oldMsgResp = oldMessages.get(i);

                if (!newMsgResp.equals(oldMsgResp)) {
                    logDiffForConvResp(userId, "messages(" + i + ")", newMsgResp.toString(), oldMsgResp.toString());
                }
            }
        }
    }

    public void postBoxUnreadCountsDiff(String userId,
                                        CompletableFuture<PostBoxUnreadCounts> newUnreadCountsFuture,
                                        CompletableFuture<PostBoxUnreadCounts> oldUnreadCountsFuture) {

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

    private void logDiffForPbResp(String params, String fieldName, String newValue, String oldValue) {
        logDiff("postBoxResponseDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiffForConvResp(String params, String fieldName, String newValue, String oldValue) {
        logDiff("conversationResponseDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiffForUnreadCounts(String params, String fieldName, String newValue, String oldValue) {
        logDiff("unreadCountsDiff", params, fieldName, newValue, oldValue);
    }

    private void logDiff(String methodName, String params, String fieldName, String newValue, String oldValue) {
        LOGGER.error(String.format("%s(%s) - %s - new: '%s' vs old: '%s'", methodName, params, fieldName, newValue, oldValue));
    }
}