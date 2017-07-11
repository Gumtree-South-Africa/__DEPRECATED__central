package com.ecg.replyts.core.runtime.workers;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WorkerPoolManagerTest {
    private Runnable mailDataProvider = new Runnable() {
        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException ex) {
                //do nothing
            }
        }
    };

    private Runnable exceptionThrowingMailDataProvider = new Runnable() {
        private int count = 0;

        @Override
        public void run() {
            if (count == 0) {
                count++;
                throw new RuntimeException("Something went wrong!");
            } else {
                count++;
            }
        }
    };

    private List<WorkerPoolManager.PersistentTask> taskList;

    @After
    public void shutDown() {
        if (taskList != null) {
            for (WorkerPoolManager.PersistentTask task : taskList) {
                task.shutdown();
            }
        }
    }

    @Test
    public void createPoolShouldSetupAndStartPool() throws InterruptedException {
        Runnable wrapper = spy(mailDataProvider);
        WorkerPoolManager manager = new WorkerPoolManager();
        ReflectionTestUtils.setField(manager, "mailDataProvider", wrapper);
        ReflectionTestUtils.setField(manager, "poolSize", 5);
        manager.createPool();
        taskList = (List) ReflectionTestUtils.getField(manager, "taskPool");
        assertThat(taskList.size(), equalTo(5));
        TimeUnit.SECONDS.sleep(1L);
        verify(wrapper, atLeast(5)).run();
    }

    @Test
    public void stopPoolShutsDownPoolProperly() throws InterruptedException {
        WorkerPoolManager manager = new WorkerPoolManager();
        ReflectionTestUtils.setField(manager, "mailDataProvider", mailDataProvider);
        ReflectionTestUtils.setField(manager, "poolSize", 3);
        ThreadGroup pool = (ThreadGroup) ReflectionTestUtils.getField(manager, "poolThreadGroup");
        manager.createPool();

        int activeThreadsCount = pool.activeCount();
        manager.stopPool();

        do {
            TimeUnit.MILLISECONDS.sleep(100L);
            int currentCount = getActiveThreadsCountInGroup(activeThreadsCount);
            if (currentCount == activeThreadsCount) {
                fail("Pool size does not seem to decrease! (" + activeThreadsCount + " threads left)");
            }
            activeThreadsCount = currentCount;
        } while (activeThreadsCount > 0);

    }

    private int getActiveThreadsCountInGroup(int activeThreadsCount) {
        Thread[] activeThreads = new Thread[activeThreadsCount];
        int counter = 0;
        for (Thread thread : activeThreads) {
            if (thread != null) {
                counter++;
            }
        }
        return counter;
    }


    @Test
    public void uncaughtExceptionInWorkerResultsInThreadRestart() throws InterruptedException {
        WorkerPoolManager manager = new WorkerPoolManager();
        ReflectionTestUtils.setField(manager, "mailDataProvider", exceptionThrowingMailDataProvider);
        ReflectionTestUtils.setField(manager, "poolSize", 1);
        manager.createPool();
        TimeUnit.MILLISECONDS.sleep(100L);

        taskList = (List) ReflectionTestUtils.getField(manager, "taskPool");

        int execCount = (Integer) ReflectionTestUtils.getField(exceptionThrowingMailDataProvider, "count");
        assertThat(execCount, Matchers.greaterThanOrEqualTo(2));
    }
}