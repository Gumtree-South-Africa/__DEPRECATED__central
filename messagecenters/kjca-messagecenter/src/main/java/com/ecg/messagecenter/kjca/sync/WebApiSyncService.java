package com.ecg.messagecenter.kjca.sync;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.service.CassandraPostBoxService;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.ecg.sync.CustomExecutorsFactory;
import com.ecg.sync.PostBoxSyncService;
import com.ecg.sync.UserIdNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

@Component
@ConditionalOnProperty(name = "webapi.sync.ca.enabled", havingValue = "true")
public class WebApiSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(WebApiSyncService.class);

    private final Counter oldModelFailureCounter = TimingReports.newCounter("webapi.diff.oldModel.failed");
    private final Counter newModelFailureCounter = TimingReports.newCounter("webapi.diff.newModel.failed");

    private final BlockService blockService;
    private final BlockUserRepository blockUserRepository;
    private final ConversationService conversationService;
    private final CassandraPostBoxService postBoxService;
    private final PostBoxSyncService postBoxSyncService;
    private final ExecutorService oldExecutor;
    private final ExecutorService newExecutor;

    // the old model is restricted to a hard limit of 500 total messages per conversation
    // (see ProcessingFinalizer#MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
    // so keeping it until we migrate to the new data model
    private final int messagesLimit;

    @Autowired
    public WebApiSyncService(
            BlockService blockService,
            BlockUserRepository blockUserRepository,
            ConversationService conversationService,
            CassandraPostBoxService postBoxService,
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            CustomExecutorsFactory customExecutorsFactory,
            @Qualifier("cassandraSessionForMb") Session session,
            @Value("${messagebox.messagesLimit:500}") int messagesLimit) {

        this.blockService = blockService;
        this.blockUserRepository = blockUserRepository;
        this.conversationService = conversationService;
        this.postBoxService = postBoxService;
        this.postBoxSyncService = new PostBoxSyncService(conversationRepository, userIdentifierService, session, postBoxRepository);
        this.messagesLimit = messagesLimit;
        this.oldExecutor = customExecutorsFactory.webApiExecutorService("old-webapi-executor");
        this.newExecutor = customExecutorsFactory.webApiExecutorService("new-webapi-executor");
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.conversationExists(email, conversationId), newExecutor)
                .thenApply(conversationIds -> postBoxSyncService.getUserId(email, conversationIds))
                .thenApply(userId -> postBoxService.markConversationAsRead(userId, conversationId, null, messagesLimit))
                .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

        CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                .supplyAsync(() -> conversationService.readConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
        return oldModelFuture.join();
    }

    public void deleteConversation(String email, String conversationId) {
        CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                .thenApply(userId -> Optional.of(postBoxService.archiveConversations(userId, Collections.singletonList(conversationId), 0, messagesLimit)))
                .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New DeleteConversation Failed - email: " + email));

        CompletableFuture<Void> oldModelFuture = CompletableFuture
                .supplyAsync(() -> conversationService.deleteConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old DeleteConversation Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
    }

    public boolean blockConversation(String email, String conversationId) {
        CompletableFuture<Void> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                .thenAccept(userId -> blockUserRepository.blockUser(email, conversationId))
                .exceptionally(handleOpt(newModelFailureCounter, "New Block User Failed - email: " + email + " conversation: " + conversationId));

        CompletableFuture<Boolean> oldModelFuture = CompletableFuture
                .supplyAsync(() -> blockService.blockConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old Block User Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
        return oldModelFuture.join();
    }

    public boolean unblockConversation(String email, String conversationId) {
        CompletableFuture<Void> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                .thenAccept(userId -> blockUserRepository.unblockUser(email, conversationId))
                .exceptionally(handleOpt(newModelFailureCounter, "New Unblock User Failed - email: " + email + " conversation: " + conversationId));

        CompletableFuture<Boolean> oldModelFuture = CompletableFuture
                .supplyAsync(() -> blockService.unblockConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old Unblock User Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
        return oldModelFuture.join();
    }

    private Function<Throwable, Void> handleOpt(Counter errorCounter, String errorMessage) {
        return ex -> {
            if (ex.getCause() instanceof UserIdNotFoundException) {
                UserIdNotFoundException userEx = (UserIdNotFoundException) ex.getCause();
                if (userEx.isLoggable()) {
                    LOG.warn("V2 Migration: " + ex.getMessage(), ex.getCause());
                }
                return null;
            } else {
                errorCounter.inc();
                LOG.error(errorMessage, ex);
                throw new RuntimeException(ex);
            }
        };
    }
}
