package com.ecg.messagecenter.diff;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.PostBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class DiffTool {

    private final Timer postBoxResponseDiffTimer = newTimer("diff.postBoxResponseDiff.timer");
//    private final Timer conversationResponseDiffTimer = newTimer("diff.conversationResponseDiff.timer");
//    private final Timer unreadCountsDiffTimer = newTimer("diff.postBoxUnreadCountsDiff.timer");

    private PostBoxResponseDiff postBoxResponseDiff;
//    private ConversationResponseDiff conversationResponseDiff;
//    private UnreadCountsDiff unreadCountsDiff;

//    @Autowired
//    public DiffTool(PostBoxResponseDiff postBoxResponseDiff,
//                    ConversationResponseDiff conversationResponseDiff,
//                    UnreadCountsDiff unreadCountsDiff) {
//        this.postBoxResponseDiff = postBoxResponseDiff;
//        this.conversationResponseDiff = conversationResponseDiff;
//        this.unreadCountsDiff = unreadCountsDiff;
//    }

    @Autowired
    public DiffTool(PostBoxResponseDiff postBoxResponseDiff) {
        this.postBoxResponseDiff = postBoxResponseDiff;
    }


    public void postBoxResponseDiff(String userId, CompletableFuture<PostBox> newFuture, CompletableFuture<PostBoxDiff> oldFuture) {
        try (Timer.Context ignored = postBoxResponseDiffTimer.time()) {
            PostBox newValue;
            PostBoxDiff oldValue;
            try {
                newValue = newFuture.join();
                oldValue = oldFuture.join();
            } catch (Throwable ignore) {
                // no point to continue with diffing
                // exception was already logged in main flow - see PostBoxServiceDelegator
                return;
            }
            postBoxResponseDiff.diff(userId, newValue, oldValue);
        }
    }

//    public void conversationResponseDiff(String userId, String conversationId,
//                                         CompletableFuture<Optional<ConversationResponse>> newConvRespFuture,
//                                         CompletableFuture<Optional<ConversationResponse>> oldConvRespFuture) {
//        try (Timer.Context ignored = conversationResponseDiffTimer.time()) {
//            Optional<ConversationResponse> newValue, oldValue;
//            try {
//                newValue = newConvRespFuture.join();
//                oldValue = oldConvRespFuture.join();
//            } catch (Throwable ignore) {
//                // no point to continue with diffing
//                // exception was already logged in main flow - see PostBoxServiceDelegator
//                return;
//            }
//            conversationResponseDiff.diff(userId, conversationId, newValue, oldValue);
//        }
//    }
//
//    public void postBoxUnreadCountsDiff(String userId,
//                                        CompletableFuture<PostBoxUnreadCounts> newUnreadCountsFuture,
//                                        CompletableFuture<PostBoxUnreadCounts> oldUnreadCountsFuture) {
//        try (Timer.Context ignored = unreadCountsDiffTimer.time()) {
//            PostBoxUnreadCounts newValue, oldValue;
//            try {
//                newValue = newUnreadCountsFuture.join();
//                oldValue = oldUnreadCountsFuture.join();
//            } catch (Throwable ignore) {
//                // no point to continue with diffing
//                // exception was already logged in main flow - see PostBoxServiceDelegator
//                return;
//            }
//            unreadCountsDiff.diff(userId, newValue, oldValue);
//        }
//    }
}