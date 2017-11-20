package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.ConversationService;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
@ConditionalOnProperty(name = "webapi.sync.uk.enabled", havingValue = "true")
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

    private final DiffTool diff;
    private final ConversationService conversationService;
    private final PostBoxService postBoxService;
    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
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

    @Autowired
    public WebApiSyncService(
            DiffTool diff,
            ConversationService conversationService,
            PostBoxService postBoxService,
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            @Value("${messagebox.messagesLimit:500}") int messagesLimit,
            @Value("${webapi.diff.executor.corePoolSize:5}") int corePoolSize,
            @Value("${webapi.diff.executor.maxPoolSize:50}") int maxPoolSize,
            @Value("${webapi.diff.executor.differ.corePoolSize:5}") int diffCorePoolSize,
            @Value("${webapi.diff.executor.differ.maxPoolSize:50}") int diffMaxPoolSize,
            @Value("${webapi.diff.executor.differ.maxQueueSize:500}") int diffMaxQueueSize,
            @Value("${webapi.diff.uk.enabled:false}") boolean diffEnabled) {

        this.diff = diff;
        this.conversationService = conversationService;
        this.postBoxService = postBoxService;
        this.postBoxRepository = postBoxRepository;
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
    }

    public PostBoxResponse getPostBox(PostBoxId id, int page, int size, boolean newCounterMode) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            CompletableFuture<PostBox> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxService.getConversations(id.asString(), Visibility.ACTIVE, page, size), newExecutor)
                    .exceptionally(handle(newModelFailureCounter, "New GetPostBox Failed - email: " + id));

            responseBuilder.buildPostBoxResponse(id.asString(), size, page, postBoxRepository.byId(id), newCounterMode);

            CompletableFuture<PostBoxDiff> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxRepository.byId(id), oldExecutor)
                    .thenApply(postBox -> PostBoxDiff.of(postBox, responseBuilder.buildPostBoxResponse(id.asString(), size, page, postBox, newCounterMode)))
                    .exceptionally(handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + id));

            if (diffEnabled) {
                CompletableFuture
                        .runAsync(() -> diff.postBoxResponseDiff(id.asString(), newModelFuture, oldModelFuture), diffExecutor)
                        .exceptionally(handleDiff("Postbox diffing Failed - email: " + id));
            }

            return oldModelFuture.join().postBoxResponse;
        }
    }

    public Optional<PostBoxSingleConversationThreadResponse> getConversation(String email, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxService.getConversation(email, conversationId, Optional.empty(), messagesLimit), newExecutor)
                    .exceptionally(handle(newModelFailureCounter, "New GetConversation Failed - email: " + email));

            CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.getConversation(email, conversationId), oldExecutor)
                    .exceptionally(handle(oldModelFailureCounter, "Old GetConversation Failed - email: " + email));

            if (diffEnabled) {
                CompletableFuture
                        .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture, oldModelFuture), diffExecutor)
                        .exceptionally(handleDiff("Conversation diffing Failed - email: " + email));
            }

            return oldModelFuture.join();
        }
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxService.markConversationAsRead(email, conversationId, Optional.empty(), messagesLimit), newExecutor)
                    .exceptionally(handle(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

            CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.readConversation(email, conversationId), oldExecutor)
                    .exceptionally(handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

            if (diffEnabled) {
                CompletableFuture
                        .runAsync(() -> diff.conversationResponseDiff(email, conversationId, newModelFuture, oldModelFuture), diffExecutor)
                        .exceptionally(handleDiff("Conversation diffing Failed - email: " + email));
            }

            return oldModelFuture.join();
        }
    }

    public Optional<ConversationRts> deleteConversation(String email, String conversationId) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {
            CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxService.changeConversationVisibilities(email, Collections.singletonList(conversationId),
                            Visibility.ARCHIVED, Visibility.ACTIVE, 0, messagesLimit), newExecutor)
                    .thenApply(postbox -> postbox.getConversations().stream().filter(conversation -> conversation.getId().equals(conversationId)).findFirst())
                    .exceptionally(handle(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

            CompletableFuture<Optional<ConversationRts>> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> conversationService.deleteConversation(email, conversationId), oldExecutor)
                    .exceptionally(handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

            if (diffEnabled) {
                CompletableFuture
                        .runAsync(() -> diff.conversationDeleteResponseDiff(email, conversationId, newModelFuture, oldModelFuture), diffExecutor)
                        .exceptionally(handleDiff("Conversation diffing Failed - email: " + email));
            }

            return oldModelFuture.join();
        }
    }

    private <T> Function<Throwable, ? extends T> handle(Counter errorCounter, String errorMessage) {
        return ex -> {
            errorCounter.inc();
            LOGGER.error(errorMessage, ex);
            throw new RuntimeException(ex);
        };
    }

    private Function<Throwable, ? extends Void> handleDiff(String errorMessage) {
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
