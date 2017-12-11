package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.CassandraPostBoxService;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.ConversationService;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
@ConditionalOnProperty(name = "webapi.sync.ek.enabled", havingValue = "true")
public class WebApiSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApiSyncService.class);
    private static final Logger DIFF_LOGGER = LoggerFactory.getLogger("diffLogger");

    private final Timer getConversationTimer = newTimer("postBoxDelegatorService.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxDelegatorService.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxDelegatorService.getConversations");
    private final Timer deleteConversationsTimer = newTimer("postBoxDelegatorService.deleteConversations");

    private final Counter oldModelFailureCounter = TimingReports.newCounter("webapi.diff.oldModel.failed");
    private final Counter newModelFailureCounter = TimingReports.newCounter("webapi.diff.newModel.failed");
    private final Counter diffFailureCounter = TimingReports.newCounter("webapi.diff.failed");

    private boolean diffEnabled;

    @Autowired(required = false)
    private DiffTool diff;

    private final Session session;
    private final ConversationService conversationService;
    private final CassandraPostBoxService postBoxService;
    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
    private final ConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int diffCorePoolSize;
    private final int diffMaxPoolSize;
    private final int diffMaxQueueSize;
    private final ExecutorService oldExecutor;
    private final ExecutorService newExecutor;
    private final ExecutorService diffExecutor;

    // the old model is restricted to a hard limit of 500 total messages per conversation
    // (see ProcessingFinalizer#MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
    // so keeping it until we migrate to the new data model
    private final int messagesLimit;

    private final PreparedStatement conversationStatement;

    @Autowired
    public WebApiSyncService(
            ConversationService conversationService,
            CassandraPostBoxService postBoxService,
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            @Qualifier("cassandraSessionForMb") Session session,
            @Value("${messagebox.messagesLimit:500}") int messagesLimit,
            @Value("${webapi.diff.executor.corePoolSize:5}") int corePoolSize,
            @Value("${webapi.diff.executor.maxPoolSize:50}") int maxPoolSize,
            @Value("${webapi.diff.executor.differ.corePoolSize:5}") int diffCorePoolSize,
            @Value("${webapi.diff.executor.differ.maxPoolSize:50}") int diffMaxPoolSize,
            @Value("${webapi.diff.executor.differ.maxQueueSize:500}") int diffMaxQueueSize,
            @Value("${webapi.diff.uk.enabled:false}") boolean diffEnabled) {

        this.conversationService = conversationService;
        this.postBoxService = postBoxService;
        this.postBoxRepository = postBoxRepository;
        this.session = session;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
        this.messagesLimit = messagesLimit;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.diffCorePoolSize = diffCorePoolSize;
        this.diffMaxPoolSize = diffMaxPoolSize;
        this.diffMaxQueueSize = diffMaxQueueSize;
        this.diffEnabled = diffEnabled;
        this.oldExecutor = newExecutorService("old-webapi-executor");
        this.newExecutor = newExecutorService("new-webapi-executor");
        this.diffExecutor = newExecutorServiceForDiff();

        this.conversationStatement = this.session.prepare("SELECT conversation_id FROM mb_postbox WHERE postbox_id = ? LIMIT 1");
    }

    /**
     * Method is not a mutator that means that if the diffing is not enabled then there is no need to use executor service and calling
     * MessageBox API. Code is executed synchronously and called directly.
     */
    public PostBoxResponse getPostBox(String email, int page, int size) {
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
                        .supplyAsync(() -> getConversationId(email), newExecutor)
                        .thenApply(conversationId -> getUserId(email, conversationId))
                        .thenApply(userId -> Optional.of(postBoxService.getConversations(userId, Visibility.ACTIVE, page * size, size)))
                        .exceptionally(handleOpt(newModelFailureCounter, "New GetPostBox Failed - email: " + email));

                CompletableFuture<PostBoxDiff> oldModelFuture = CompletableFuture
                        .supplyAsync(() -> postBoxRepository.byId(PostBoxId.fromEmail(email)), oldExecutor)
                        .thenApply(postBox -> PostBoxDiff.of(postBox, responseBuilder.buildPostBoxResponse(email, size, page, postBox)))
                        .exceptionally(handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + email));

                CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
                if (newModelFuture.join().isPresent()) {
                    CompletableFuture
                            .runAsync(() -> diff.postBoxResponseDiff(email, newModelFuture.join().get(), oldModelFuture.join()), diffExecutor)
                            .exceptionally(handleDiff("Postbox diffing Failed - email: " + email));
                }

                response = oldModelFuture.join().postBoxResponse;
            } else {
                com.ecg.messagecenter.persistence.simple.PostBox postBox;
                if (size != 0) {
                    postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                } else {
                    postBox = postBoxRepository.byIdWithoutConversationThreads(PostBoxId.fromEmail(email));
                }

                response = responseBuilder.buildPostBoxResponse(email, size, page, postBox);
            }

            return response;
        }
    }

    /**
     * Method is a mutator that means that 'readPostbox' change an internal state of the postbox. Therefore, MessageBox API must be
     * called even if the diffing is not enable because of changing the internal state.
     */
    public PostBoxResponse readPostBox(String email, int page, int size) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            Function<com.ecg.messagecenter.persistence.simple.PostBox, com.ecg.messagecenter.persistence.simple.PostBox> readPostbox =
                    (com.ecg.messagecenter.persistence.simple.PostBox postBox) -> {
                        List conversations = postBox.getConversationThreadsCapTo(page, size);
                        int totalUnreads = postBoxRepository.unreadCountInConversations(postBox.getId(), conversations);
                        postBox.decrementNewReplies(totalUnreads);
                        postBoxRepository.markConversationsAsRead(postBox, conversations);
                        return postBox;
                    };

            /*
             * First two calls are dedicated to find real UserID using the email:
             * - find first conversation using V1
             * - find created event to this conversation and look into custom headers to resolve real UserID belonging to this email
             * - if user does not have any conversation in V1 or UserID is not found in conversation event then UserIdNotFound is thrown
             * and Warning is logged
             */

            CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> getConversationId(email), newExecutor)
                    .thenApply(conversationId -> getUserId(email, conversationId))
                    .thenApply(userId -> Optional.of(postBoxService.markConversationsAsRead(userId, Visibility.ACTIVE, page, size)))
                    .exceptionally(handleOpt(newModelFailureCounter, "New ReadPostBox Failed - email: " + email));

            CompletableFuture<PostBoxDiff> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxRepository.byId(PostBoxId.fromEmail(email)), oldExecutor)
                    .thenApply(readPostbox)
                    .thenApply(postBox -> PostBoxDiff.of(postBox, responseBuilder.buildPostBoxResponse(email, size, page, postBox)))
                    .exceptionally(handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + email));

            CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
            if (diffEnabled && newModelFuture.join().isPresent()) {
                CompletableFuture
                        .runAsync(() -> diff.postBoxResponseDiff(email, newModelFuture.join().get(), oldModelFuture.join()), diffExecutor)
                        .exceptionally(handleDiff("Postbox diffing Failed - email: " + email));
            }

            return oldModelFuture.join().postBoxResponse;
        }
    }

    /**
     * Method is a mutator that means that 'deleteConversation' change an internal state of the conversion. Therefore, MessageBox API must be
     * called even if the diffing is not enable because of changing the internal state.
     * <p>
     * Diffing cannot be used because there is not easy way how to get a deleted conversation without deeper changes or an DB additional call.
     */
    public PostBoxResponse deleteConversations(String email, List<String> ids, int page, int size) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> getConversationId(email), newExecutor)
                    .thenApply(conversationId -> getUserId(email, conversationId))
                    .thenApply(userId -> Optional.of(deleteConversationV2(userId, ids)))
                    .exceptionally(handleOpt(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

            CompletableFuture<PostBoxResponse> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxRepository.byId(PostBoxId.fromEmail(email)), oldExecutor)
                    .thenApply(postbox -> {
                        postBoxRepository.deleteConversations(postbox, ids);
                        return postbox;
                    })
                    .thenApply(postBox -> responseBuilder.buildPostBoxResponse(email, size, page, postBox))
                    .exceptionally(handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

            CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
            return oldModelFuture.join();
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
                        .supplyAsync(() -> getUserId(email, conversationId), newExecutor)
                        .thenApply(userId -> postBoxService.getConversation(userId, conversationId, Optional.empty(), messagesLimit))
                        .exceptionally(handleOpt(newModelFailureCounter, "New GetConversation Failed - email: " + email));

                CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                        .supplyAsync(() -> conversationService.getConversation(email, conversationId), oldExecutor)
                        .exceptionally(handle(oldModelFailureCounter, "Old GetConversation Failed - email: " + email));

                CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
                if (newModelFuture.join().isPresent()) {
                    CompletableFuture
                            .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture.join(), oldModelFuture.join()), diffExecutor)
                            .exceptionally(handleDiff("Conversation diffing Failed - email: " + email));
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
                    .supplyAsync(() -> getUserId(email, conversationId), newExecutor)
                    .thenApply(userId -> postBoxService.markConversationAsRead(userId, conversationId, Optional.empty(), messagesLimit))
                    .exceptionally(handleOpt(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

            CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.readConversation(email, conversationId), oldExecutor)
                    .exceptionally(handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

            CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
            if (diffEnabled && newModelFuture.join().isPresent()) {
                CompletableFuture
                        .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture.join(), oldModelFuture.join()), diffExecutor)
                        .exceptionally(handleDiff("Conversation diffing Failed - email: " + email));
            }

            return oldModelFuture.join();
        }
    }

    private PostBox deleteConversationV2(String userId, List<String> conversationIds) {
        return postBoxService.changeConversationVisibilities(userId, conversationIds,
                Visibility.ARCHIVED, Visibility.ACTIVE, 0, messagesLimit);
    }

    private String getConversationId(String email) {
        ResultSet resultSet = session.execute(conversationStatement.bind(email));
        if (resultSet.iterator().hasNext()) {
            Row row = resultSet.iterator().next();
            return row.getString("conversation_id");
        }

        throw new UserIdNotFoundException(email);
    }

    private String getUserId(String email, String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        Optional<String> userId;
        String userRole;
        if (conversation != null && email.equals(conversation.getBuyerId())) {
            userId = userIdentifierService.getBuyerUserId(conversation);
            userRole = "buyer";
        } else if (conversation != null && email.equals(conversation.getSellerId())) {
            userId = userIdentifierService.getSellerUserId(conversation);
            userRole = "seller";
        } else {
            userId = Optional.empty();
            userRole = UserIdNotFoundException.UNKNOWN;
        }

        return userId.orElseThrow(() -> new UserIdNotFoundException(email, userRole, conversationId));
    }

    private <T> Function<Throwable, Optional<T>> handleOpt(Counter errorCounter, String errorMessage) {
        return ex -> {
            if (ex.getCause() instanceof UserIdNotFoundException) {
                LOGGER.warn(ex.getMessage(), ex.getCause());
                return Optional.empty();
            } else {
                errorCounter.inc();
                LOGGER.error(errorMessage, ex);
                throw new RuntimeException(ex);
            }
        };
    }

    private <T> Function<Throwable, T> handle(Counter errorCounter, String errorMessage) {
        return ex -> {
            errorCounter.inc();
            LOGGER.error(errorMessage, ex);
            throw new RuntimeException(ex);
        };
    }

    private Function<Throwable, Void> handleDiff(String errorMessage) {
        return ex -> {
            diffFailureCounter.inc();
            DIFF_LOGGER.error(errorMessage, ex);
            return null;
        };
    }

    private ExecutorService newExecutorService(String metricsName) {
        String metricsOwner = "webapi-diff-service";
        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(), new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)),
                metricsOwner,
                metricsName);
    }

    private ExecutorService newExecutorServiceForDiff() {
        BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(diffCorePoolSize, diffCorePoolSize, diffMaxQueueSize);
        TimingReports.newGauge("webapi.diffExecutor.queueSizeGauge", (Gauge<Integer>) queue::size);

        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(diffCorePoolSize, diffMaxPoolSize, 60L, TimeUnit.SECONDS, queue),
                "webapiDiffService", "diffExecutor");
    }
}
