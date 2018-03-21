package com.ecg.sync;

import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class CustomExecutorsFactory {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int diffCorePoolSize;
    private final int diffMaxPoolSize;
    private final int diffMaxQueueSize;

    public CustomExecutorsFactory(
            @Value("${webapi.diff.executor.corePoolSize:5}") int corePoolSize,
            @Value("${webapi.diff.executor.maxPoolSize:50}") int maxPoolSize,
            @Value("${webapi.diff.executor.differ.corePoolSize:5}") int diffCorePoolSize,
            @Value("${webapi.diff.executor.differ.maxPoolSize:50}") int diffMaxPoolSize,
            @Value("${webapi.diff.executor.differ.maxQueueSize:500}") int diffMaxQueueSize
    ) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.diffCorePoolSize = diffCorePoolSize;
        this.diffMaxPoolSize = diffMaxPoolSize;
        this.diffMaxQueueSize = diffMaxQueueSize;
    }

    public ExecutorService webApiExecutorService(String metricsName) {
        String metricsOwner = "webapi-diff-service";
        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(), new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)),
                metricsOwner,
                metricsName);
    }

    public ExecutorService webApiDiffExecutorService() {
        BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(diffCorePoolSize, diffCorePoolSize, diffMaxQueueSize);
        TimingReports.newGauge("webapi.diffExecutor.queueSizeGauge", queue::size);

        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(diffCorePoolSize, diffMaxPoolSize, 60L, TimeUnit.SECONDS, queue),
                "webapiDiffService", "diffExecutor");
    }
}
