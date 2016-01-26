package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Adapter to wrap a single check as MBean.
 *
 * @author smoczarski
 * @author Peter Rossbach (pr@objektpark.de)
 */
public class SingleCheckAdapter extends AbstractCheckAdapter implements SingleCheckAdapterMBean {

    private final Check check;

    // Every check has its own executor for performing the check.
    private final ExecutorService executor;

    /**
     * Timeout for performing the check (in ms)
     */
    private long timeout = 5000;

    private static final String NAME_PREFIX = "Sanity-Check ";

    private Future<Result> lastFeature;

    private static final Logger LOG = LoggerFactory.getLogger(SingleCheckAdapter.class);

    /**
     * Create a adapter for a check.
     *
     * @param check
     */
    public SingleCheckAdapter(Check check) {
        this.check = check;
        executor = Executors.newFixedThreadPool(1, new ThreadFactory() {

            public Thread newThread(Runnable r) {

                Thread thread = new Thread(r, NAME_PREFIX + getName());
                thread.setDaemon(true); // Don't prevent VM shutdown

                return thread;
            }
        });
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return check.getName();
        /*
        String subCategory = check.getSubCategory() == null ? "" : check.getSubCategory();
        return check.getCategory() + ":" + subCategory + ":" + check.getName();
        */
    }

    private Result createTimeoutResult() {
        return Result.createResult(Status.CRITICAL, Message.shortInfo(String.format("timeout after %d ms", timeout)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Result performInternal() throws Exception {

        if (lastFeature != null && !lastFeature.isDone()) {
            // last check processing hang, we can't do a new check
            return createTimeoutResult();
        }
        // Execute the check in an dedicated thread. Blocking of the requestor must be prevented.
        lastFeature = executor.submit(new Callable<Result>() {

            public Result call() throws Exception {
                try {
                    return check.execute();
                } catch (RuntimeException e) {
                    LOG.error("Runtime Exception while executing check: " + check.getClass(), e);
                    throw e;
                }
            }
        });
        Result result = null;

        try {
            result = lastFeature.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = Result.createResult(Status.CRITICAL, Message.shortInfo("interrupted"));
        } catch (TimeoutException e) {
            result = createTimeoutResult();
        }
        return result;
    }


    /**
     * Release resources.
     */
    public void destroy() {
        executor.shutdownNow();
    }

    @Override
    public String getCategory() {
        if (check == null) return null;
        return check.getCategory();
    }

    @Override
    public String getSubCategory() {
        if (check == null) return null;
        return check.getSubCategory();
    }

}
