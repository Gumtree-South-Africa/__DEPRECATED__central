package com.ecg.replyts.app.mailreceiver;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Component
public class MessageProcessingPoolManager {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessingPoolManager.class);

    private final int mailProcessingThreads;
    private final ListeningExecutorService executor;
    private final MessageProcessor messageProcessor;
    private final Duration gracefulShutdownDuration;

    @Autowired
    public MessageProcessingPoolManager(@Value("${replyts.threadpool.size:2}") int mailProcessingThreads,
                                        @Value("${replyts.threadpool.shutdown.await.ms:10000}") long gracefulShutdownTimeoutMs,
                                        @Qualifier("mailDataProvider") MessageProcessor messageProcessor) {
        checkArgument(mailProcessingThreads > 0, "mailProcessingThreads <= 0");
        checkArgument(gracefulShutdownTimeoutMs > 0, "gracefulShutdownTimeoutMs <= 0");
        this.mailProcessingThreads = mailProcessingThreads;
        this.gracefulShutdownDuration = Duration.ofMillis(gracefulShutdownTimeoutMs);
        this.messageProcessor = messageProcessor;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(mailProcessingThreads,
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("ReplyTS-worker-thread-%d")
                        .build()
        ));
    }

    @PostConstruct
    public void startProcessing() {
        checkState(!executor.isShutdown(), "The mail processing pool has been shutdown");
        Stream.generate(this::createWorker).limit(mailProcessingThreads).forEach(this::submitWorker);
    }

    private Runnable createWorker() {
        return setTaskFields(() -> {
            try {
                messageProcessor.processNext();
            } catch (InterruptedException e) {
                LOG.warn("The worker thread has been interrupted");
                Thread.currentThread().interrupt();
            }
        }, "ReplyTS-worker-thread");
    }

    private void submitWorker(Runnable worker) {
        if (executor.isShutdown()) {
            LOG.warn("The executor is shut down, not going to submit the task");
            return;
        }
        ListenableFuture<?> completion = executor.submit(worker);
        Futures.addCallback(completion, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                submitWorker(worker);
            }

            @Override
            public void onFailure(@Nonnull Throwable t) {
                LOG.error("Uncaught exception from the worker thread", t);
                submitWorker(worker);
            }
        });
    }

    @PreDestroy
    public void stopProcessing() {
        executor.shutdown();
        try {
            LOG.info("Awaiting {}ms for the mail processing threads termination...", gracefulShutdownDuration.toMillis());
            if (!executor.awaitTermination(gracefulShutdownDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                LOG.warn("Some of the thread haven't completed during the graceful period, going to interrupt them...");
            }
            executor.shutdownNow();
            LOG.info("The mail processing service executor has been stopped (the threads may've not)");
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for the mail processing threads termination");
            Thread.currentThread().interrupt();
        }
    }
}
