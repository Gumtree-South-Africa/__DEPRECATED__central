package com.ecg.messagecenter.diff;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Component
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class DiffTool {

    private PostBoxResponseDiff postBoxResponseDiff;
    private ConversationResponseDiff conversationResponseDiff;
    private final ConversationDeleteResponseDiff conversationDeleteResponseDiff;

    @Autowired
    public DiffTool(PostBoxResponseDiff postBoxResponseDiff,
                    ConversationResponseDiff conversationResponseDiff,
                    ConversationDeleteResponseDiff conversationDeleteResponseDiff) {
        this.postBoxResponseDiff = postBoxResponseDiff;
        this.conversationResponseDiff = conversationResponseDiff;
        this.conversationDeleteResponseDiff = conversationDeleteResponseDiff;
    }


    void postBoxResponseDiff(String userId, CompletableFuture<PostBox> newFuture, CompletableFuture<PostBoxDiff> oldFuture) {
        processFuture((newValue, oldValue) -> postBoxResponseDiff.diff(userId, newValue, oldValue), newFuture, oldFuture);
    }

    void conversationResponseDiff(String userId, String conversationId,
                                  CompletableFuture<Optional<ConversationThread>> newFuture,
                                  CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldFuture) {
        processFuture((newValue, oldValue) -> conversationResponseDiff.diff(userId, conversationId, newValue, oldValue), newFuture, oldFuture);
    }

    void conversationDeleteResponseDiff(String userId, String conversationId,
                                        CompletableFuture<Optional<ConversationThread>> newFuture,
                                        CompletableFuture<Optional<ConversationRts>> oldFuture) {
        processFuture((newValue, oldValue) -> conversationDeleteResponseDiff.diff(userId, conversationId, newValue, oldValue), newFuture, oldFuture);
    }

    private static <T, U> void processFuture(BiConsumer<T, U> consumer,
                                             CompletableFuture<T> newFuture,
                                             CompletableFuture<U> oldFuture) {
        T newValue;
        U oldValue;
        try {
            newValue = newFuture.join();
            oldValue = oldFuture.join();
        } catch (Throwable ignore) {
            // no point to continue with diffing
            // exception was already logged in main flow - see PostBoxServiceDelegator
            return;
        }
        consumer.accept(newValue, oldValue);
    }
}