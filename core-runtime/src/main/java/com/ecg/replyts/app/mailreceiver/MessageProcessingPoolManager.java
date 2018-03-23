package com.ecg.replyts.app.mailreceiver;

import com.ecg.replyts.core.ApplicationReadyEvent;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.NotThreadSafe;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Component
@NotThreadSafe
public abstract class MessageProcessingPoolManager {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessingPoolManager.class);

    protected final ListeningExecutorService executor;
    private final Duration gracefulShutdownDuration;

    private Set<MessageProcessor> messageProcessors = new HashSet<>();

    public MessageProcessingPoolManager(int nrOfExecutors, long gracefulShutdownTimeoutMs) {
        checkArgument(nrOfExecutors > 0, "nrOfExecutors <= 0");
        checkArgument(gracefulShutdownTimeoutMs > 0, "gracefulShutdownTimeoutMs <= 0");
        this.gracefulShutdownDuration = Duration.ofMillis(gracefulShutdownTimeoutMs);
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nrOfExecutors,
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("ReplyTS-worker-thread-%d")
                        .build()
        ));
    }

    private Runnable createWorker(MessageProcessor messageProcessor) {
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
        for (MessageProcessor messageProcessor : messageProcessors) {
            try {
                messageProcessor.destroy();
            } catch (Exception e) {
                LOG.error("Was not able to clean up message processor", e);
            }
        }
        messageProcessors.clear();

        executor.shutdown();
        try {
            LOG.info("Awaiting {}ms for the mail processing threads termination...", gracefulShutdownDuration.toMillis());
            if (!executor.awaitTermination(gracefulShutdownDuration.toMillis(), TimeUnit.MILLISECONDS)) {
                LOG.warn("Some of the thread haven't completed during the graceful period, going to interrupt them...");
            }
            executor.shutdownNow();
            LOG.info("The mail processing service executor has been stopped, the threads may not have");
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for the mail processing threads termination");
            Thread.currentThread().interrupt();
        }
    }

    protected abstract Stream<MessageProcessor> createProcessorStream();

    @EventListener
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        checkState(!executor.isShutdown(), "The mail processing pool has been shutdown");
        createProcessorStream().peek(messageProcessors::add).map(this::createWorker).forEach(this::submitWorker);
    }
}
