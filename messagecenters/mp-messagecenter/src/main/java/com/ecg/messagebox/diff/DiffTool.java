package com.ecg.messagebox.diff;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

/**
 * Diff tool that checks and logs differences between old model and new model responses.
 */
@Component
public class DiffTool {

    private final Timer postBoxResponseDiffTimer = newTimer("diff.postBoxResponseDiff.timer");
    private final Timer conversationResponseDiffTimer = newTimer("diff.conversationResponseDiff.timer");
    private final Timer unreadCountsDiffTimer = newTimer("diff.postBoxUnreadCountsDiff.timer");

    private PostBoxResponseDiff postBoxResponseDiff;
    private ConversationResponseDiff conversationResponseDiff;
    private UnreadCountsDiff unreadCountsDiff;

    @Autowired
    public DiffTool(PostBoxResponseDiff postBoxResponseDiff,
                    ConversationResponseDiff conversationResponseDiff,
                    UnreadCountsDiff unreadCountsDiff) {
        this.postBoxResponseDiff = postBoxResponseDiff;
        this.conversationResponseDiff = conversationResponseDiff;
        this.unreadCountsDiff = unreadCountsDiff;
    }

    public void postBoxResponseDiff(String userId,
                                    CompletableFuture<PostBoxResponse> newPbResponseFuture,
                                    CompletableFuture<PostBoxResponse> oldPbResponseFuture) {
        try (Timer.Context ignored = postBoxResponseDiffTimer.time()) {
            PostBoxResponse newValue, oldValue;
            try {
                newValue = newPbResponseFuture.join();
                oldValue = oldPbResponseFuture.join();
            } catch (Throwable ignore) {
                // no point to continue with diffing
                // exception was already logged in main flow - see PostBoxServiceDelegator
                return;
            }
            postBoxResponseDiff.diff(userId, newValue, oldValue);
        }
    }

    public void conversationResponseDiff(String userId, String conversationId,
                                         CompletableFuture<Optional<ConversationResponse>> newConvRespFuture,
                                         CompletableFuture<Optional<ConversationResponse>> oldConvRespFuture) {
        try (Timer.Context ignored = conversationResponseDiffTimer.time()) {
            Optional<ConversationResponse> newValue, oldValue;
            try {
                newValue = newConvRespFuture.join();
                oldValue = oldConvRespFuture.join();
            } catch (Throwable ignore) {
                // no point to continue with diffing
                // exception was already logged in main flow - see PostBoxServiceDelegator
                return;
            }
            conversationResponseDiff.diff(userId, conversationId, newValue, oldValue);
        }
    }

    public void postBoxUnreadCountsDiff(String userId,
                                        CompletableFuture<PostBoxUnreadCounts> newUnreadCountsFuture,
                                        CompletableFuture<PostBoxUnreadCounts> oldUnreadCountsFuture) {
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
            unreadCountsDiff.diff(userId, newValue, oldValue);
        }
    }
}