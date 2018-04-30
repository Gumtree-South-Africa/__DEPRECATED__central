package com.ecg.messagecenter.gtuk.diff;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.CassandraPostBoxService;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.gtuk.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.gtuk.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.sync.CustomExecutorsFactory;
import com.ecg.sync.PostBoxDiff;
import com.ecg.sync.PostBoxResponse;
import com.ecg.sync.PostBoxSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
@ConditionalOnProperty(name = "webapi.sync.uk.enabled", havingValue = "true")
public class WebApiSyncService {

    private final Timer getConversationTimer = newTimer("postBoxDelegatorService.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxDelegatorService.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxDelegatorService.getConversations");
    private final Timer deleteConversationsTimer = newTimer("postBoxDelegatorService.deleteConversations");

    private final Counter oldModelFailureCounter = TimingReports.newCounter("webapi.diff.oldModel.failed");
    private final Counter newModelFailureCounter = TimingReports.newCounter("webapi.diff.newModel.failed");

    private boolean diffEnabled;

    @Autowired(required = false)
    private DiffTool diff;

    private final ConversationService conversationService;
    private final CassandraPostBoxService postBoxService;
    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
    private final PostBoxSyncService postBoxSyncService;
    private final ExecutorService oldExecutor;
    private final ExecutorService newExecutor;
    private final ExecutorService diffExecutor;

    // the old model is restricted to a hard limit of 500 total messages per conversation
    // (see ProcessingFinalizer#MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
    // so keeping it until we migrate to the new data model
    private final int messagesLimit;

    @Autowired
    public WebApiSyncService(
            ConversationService conversationService,
            CassandraPostBoxService postBoxService,
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            CustomExecutorsFactory customExecutorsFactory,
            @Qualifier("cassandraSessionForMb") Session session,
            @Value("${messagebox.messagesLimit:500}") int messagesLimit,
            @Value("${webapi.diff.uk.enabled:false}") boolean diffEnabled) {

        this.conversationService = conversationService;
        this.postBoxService = postBoxService;
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
        this.postBoxSyncService = new PostBoxSyncService(conversationRepository, userIdentifierService, session, postBoxRepository);
        this.messagesLimit = messagesLimit;
        this.diffEnabled = diffEnabled;
        this.oldExecutor = customExecutorsFactory.webApiExecutorService("old-webapi-executor");
        this.newExecutor = customExecutorsFactory.webApiExecutorService("new-webapi-executor");
        this.diffExecutor = customExecutorsFactory.webApiDiffExecutorService();
    }

    /**
     * Method is not a mutator that means that if the diffing is not enabled then there is no need to use executor service and calling
     * MessageBox API. Code is executed synchronously and called directly.
     */
    public PostBoxResponse getPostBox(String email, int page, int size, boolean newCounterMode) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            PostBoxResponse response;
            if (diffEnabled) {
                /*
                 * First two calls are dedicated to find real UserID using the email:
                 * - find first conversation using V1
                 * - find created event to this conversation and look into custom headers to resolve real UserID belonging to this email
                 * - if user does not have any conversation in V1 or UserID is not found in conversation event then UserIdNotFound is thrown
                 * and Warning is logged
                 */
                CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                        .supplyAsync(() -> postBoxSyncService.getConversationId(email), newExecutor)
                        .thenApply(convIds -> postBoxSyncService.getUserId(email, convIds))
                        .thenApply(userId -> Optional.of(postBoxService.getConversations(userId, Visibility.ACTIVE, page * size, size)))
                        .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New GetPostBox Failed - email: " + email));

                CompletableFuture<PostBoxDiff> oldModelFuture = CompletableFuture
                        .supplyAsync(() -> postBoxRepository.byId(PostBoxId.fromEmail(email)), oldExecutor)
                        .thenApply(postBox -> new PostBoxDiff(postBox, responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode)))
                        .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + email));

                CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
                if (newModelFuture.join().isPresent()) {
                    CompletableFuture
                            .runAsync(() -> diff.postBoxResponseDiff(email, newModelFuture.join().get(), oldModelFuture.join()), diffExecutor)
                            .exceptionally(postBoxSyncService.handleDiff("Postbox diffing Failed - email: " + email));
                }

                response = oldModelFuture.join().getPostBoxResponse();
            } else {
                com.ecg.messagecenter.core.persistence.simple.PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                response = responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
            }

            return response;
        }
    }

    /**
     * Method is not a mutator that means that if the diffing is not enabled then there is no need to use executor service and calling
     * MessageBox API. Code is executed synchronously and called directly.
     */
    public Optional<PostBoxSingleConversationThreadResponse> getConversation(String email, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            Optional<PostBoxSingleConversationThreadResponse> response;
            if (diffEnabled) {
                CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                        .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                        .thenApply(userId -> postBoxService.getConversation(userId, conversationId, null, messagesLimit))
                        .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New GetConversation Failed - email: " + email));

                CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                        .supplyAsync(() -> conversationService.getConversation(email, conversationId), oldExecutor)
                        .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old GetConversation Failed - email: " + email));

                CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
                if (newModelFuture.join().isPresent()) {
                    CompletableFuture
                            .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture.join(), oldModelFuture.join()), diffExecutor)
                            .exceptionally(postBoxSyncService.handleDiff("Conversation diffing Failed - email: " + email));
                }

                response = oldModelFuture.join();
            } else {
                response = conversationService.getConversation(email, conversationId);
            }

            return response;
        }
    }

    /**
     * Method is a mutator that means that 'readConversation' change an internal state of the conversion. Therefore, MessageBox API must be
     * called even if the diffing is not enable because of changing the internal state.
     */
    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxSyncService.conversationExists(email, conversationId), newExecutor)
                    .thenApply(conversationIds -> postBoxSyncService.getUserId(email, conversationIds))
                    .thenApply(userId -> postBoxService.markConversationAsRead(userId, conversationId, null, messagesLimit))
                    .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

            CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.readConversation(email, conversationId), oldExecutor)
                    .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

            CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
            if (diffEnabled && newModelFuture.join().isPresent()) {
                CompletableFuture
                        .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture.join(), oldModelFuture.join()), diffExecutor)
                        .exceptionally(postBoxSyncService.handleDiff("Conversation diffing Failed - email: " + email));
            }

            return oldModelFuture.join();
        }
    }

    /**
     * Method is a mutator that means that 'deleteConversation' change an internal state of the conversion. Therefore, MessageBox API must be
     * called even if the diffing is not enable because of changing the internal state.
     * <p>
     * Diffing cannot be used because there is not easy way how to get a deleted conversation without deeper changes or an DB additional call.
     */
    public Optional<ConversationRts> deleteConversation(String email, String conversationId) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                    .thenApply(userId -> Optional.of(postBoxService.archiveConversations(userId, Collections.singletonList(conversationId), 0, messagesLimit)))
                    .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New DeleteConversation Failed - email: " + email));

            CompletableFuture<Optional<ConversationRts>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.deleteConversation(email, conversationId), oldExecutor)
                    .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old DeleteConversation Failed - email: " + email));

            CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
            return oldModelFuture.join();
        }
    }
}
