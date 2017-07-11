package com.ecg.replyts.core.runtime.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@Component
public class WorkerPoolManager {
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPoolManager.class);

    private static final String WORKER_POOL_NAME = "worker pool";
    private static final String THREAD_NAME_PREFIX = "ReplyTS-worker-thread-";

    @Autowired
    @Qualifier("mailDataProvider")
    private Runnable mailDataProvider;

    @Value("${replyts.threadpool.size:2}")
    private int poolSize;

    private List<PersistentTask> taskPool = new ArrayList<>();

    private ThreadGroup poolThreadGroup = new ThreadGroup(WORKER_POOL_NAME);

    @PostConstruct
    public void createPool() {
        for (int i = 0; i < poolSize; i++) {
            PersistentTask task = new PersistentTask();
            createAndStartThread(task, i);
            taskPool.add(task);
        }
    }

    private void createAndStartThread(PersistentTask task, int index) {
        Thread thread = new Thread(poolThreadGroup, task, THREAD_NAME_PREFIX + index);

        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler((t, e) -> {
            task.shutdown();
            createAndStartThread(task, index);
            LOG.error(format("Thread '%s' has an uncaught exception and was restarted", t.getName()), e);
        });

        thread.start();
    }

    @PreDestroy
    public void stopPool() {
        for (PersistentTask persistentTask : taskPool) {
            persistentTask.shutdown();
        }
    }

    class PersistentTask implements Runnable {
        private volatile Thread currentThread;

        @Override
        public void run() {
            currentThread = Thread.currentThread();
            while (currentThread == Thread.currentThread()) {
                mailDataProvider.run();
            }
        }

        public void shutdown() {
            currentThread = null;
        }
    }
}
