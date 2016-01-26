package com.ecg.replyts.core.runtime.workers;

import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor.BatchFailedException;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor.ErrorHandlingPolicy;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class BlockingBatchExecutorTest {


    @Test
    public void skipsFailingExecutions() {
        final AtomicInteger counter = new AtomicInteger();
        BlockingBatchExecutor<String> executor = new BlockingBatchExecutor<String>("task", 1, 1, TimeUnit.SECONDS);

        executor.executeAll(Arrays.asList("a", "b"), new Function<String, Runnable>() {
            @Nullable
            @Override
            public Runnable apply(@Nullable String input) {
                return new Runnable() {
                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        throw new RuntimeException();
                    }
                };
            }
        }, ErrorHandlingPolicy.SKIP_ERRORS);

        assertEquals(2, counter.get());
    }


    @Test
    public void stopsExecutionAfterFaileds() {
        final List<String> executed = Lists.newArrayList();
        BlockingBatchExecutor<String> executor = new BlockingBatchExecutor<String>("task", 1, 10, TimeUnit.SECONDS);

        try {
            executor.executeAll(Arrays.asList("a", "x", "b", "c"), new Function<String, Runnable>() {
                @Nullable
                @Override
                public Runnable apply(final String input) {
                    return new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException();
                            }
                            executed.add(input);
                            if (input.equals("x")) {
                                throw new RuntimeException();

                            }
                        }
                    };
                }
            }, ErrorHandlingPolicy.FAIL_FAST_ON_ERRORS);
            Assert.fail();
        } catch (BatchFailedException ex) {
            // DESIRED
        }
        assertEquals(Arrays.asList("a", "x"), executed);


    }


}
