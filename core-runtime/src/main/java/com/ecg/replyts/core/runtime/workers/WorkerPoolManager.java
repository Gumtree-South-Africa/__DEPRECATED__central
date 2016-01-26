/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.runtime.workers;

import com.ecg.replyts.app.mailreceiver.MailDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

public class WorkerPoolManager {

    private static final String WORKER_POOL_NAME = "worker pool";
    private static final String THREAD_NAME_PREFIX = "ReplyTS-worker-thread-";
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerPoolManager.class);

    private final List<PersistentTask> taskPool;
    private final int poolSize;
    private final MailDataProvider mailDataProvider;
    private final ThreadGroup poolThreadGroup;

    public WorkerPoolManager(int poolSize, final MailDataProvider mailDataProvider) {
        this.poolSize = poolSize;
        taskPool = new ArrayList<>();
        this.mailDataProvider = mailDataProvider;
        poolThreadGroup = new ThreadGroup(WORKER_POOL_NAME); // NOSONAR
    }

    @PostConstruct
    public void createPool() {
        mailDataProvider.prepareLaunch();
        for (int i = 0; i < poolSize; i++) {
            PersistentTask task = new PersistentTask();
            createAndStartThread(task, i);
            taskPool.add(task);
        }
    }

    private void createAndStartThread(final PersistentTask task, final int threadPoolIndex) {
        String threadName = THREAD_NAME_PREFIX + threadPoolIndex;
        Thread thread = new Thread(poolThreadGroup, task, threadName);
        thread.setDaemon(false);
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                task.shutdown();
                createAndStartThread(task, threadPoolIndex);
                LOGGER.error(String.format("Thread '%s' has an uncaught exception and was restarted", t.getName()), e);
            }
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
