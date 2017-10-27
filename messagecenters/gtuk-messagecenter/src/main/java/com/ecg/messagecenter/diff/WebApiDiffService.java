package com.ecg.messagecenter.diff;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
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

import java.util.concurrent.*;
import java.util.function.Function;

@Component("webApiDiffService")
@ConditionalOnProperty(name = "webapi.diff.uk.enabled", havingValue = "true")
public class WebApiDiffService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApiDiffService.class);
    private static final Logger DIFF_LOGGER = LoggerFactory.getLogger("diffLogger");

    private final Timer processNewMessageTimer = TimingReports.newTimer("webapi.diff.processNewMessage");
    private final Counter oldModelFailureCounter = TimingReports.newCounter("webapi.diff.oldModel.failed");
    private final Counter newModelFailureCounter = TimingReports.newCounter("webapi.diff.newModel.failed");
    private final Counter diffFailureCounter = TimingReports.newCounter("webapi.diff.failed");


    private final DiffTool diff;
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

    @Autowired
    public WebApiDiffService(
            DiffTool diff,
            PostBoxService postBoxService,
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            @Value("${webapi.diff.executor.corePoolSize:5}") int corePoolSize,
            @Value("${webapi.diff.executor.maxPoolSize:50}") int maxPoolSize,
            @Value("${webapi.diff.executor.corePoolSize:5}") int diffCorePoolSize,
            @Value("${webapi.diff.executor.maxPoolSize:50}") int diffMaxPoolSize,
            @Value("${webapi.diff.executor.maxQueueSize:500}") int diffMaxQueueSize) {

        this.diff = diff;
        this.postBoxService = postBoxService;
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.diffCorePoolSize = diffCorePoolSize;
        this.diffMaxPoolSize = diffMaxPoolSize;
        this.diffMaxQueueSize = diffMaxQueueSize;
        this.oldExecutor = newExecutorService("old-webapi-executor");
        this.newExecutor = newExecutorService("new-webapi-executor");
        this.diffExecutor = newExecutorServiceForDiff();
    }

    public PostBoxResponse getPostBox(PostBoxId id, int page, int size, boolean newCounterMode) {
        try (Timer.Context ignored = processNewMessageTimer.time()) {

            CompletableFuture<PostBox> newModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxService.getConversations(id.asString(), Visibility.ACTIVE, page, size), newExecutor)
                    .exceptionally(handle(newModelFailureCounter, "New GetPostBox Failed - email: " + id));

            // Retrieve PostBox from V1 and convert it to the Response
            // Wrap both object into and pass it to differ to find differences
            CompletableFuture<PostBoxDiff> oldModelFuture = CompletableFuture
                    .supplyAsync(() -> postBoxRepository.byId(id), oldExecutor)
                    .thenApply(postBox -> PostBoxDiff.of(postBox, responseBuilder.buildPostBoxResponse(id.asString(), size, page, postBox, newCounterMode)))
                    .exceptionally(handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + id));

            // Asynchronously diff new and old models
            CompletableFuture
                    .runAsync(() -> diff.postBoxResponseDiff(id.asString(), newModelFuture, oldModelFuture), diffExecutor)
                    .exceptionally(handleDiff("Postbox diffing Failed - email: " + id));

            return oldModelFuture.join().postBoxResponse;
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
