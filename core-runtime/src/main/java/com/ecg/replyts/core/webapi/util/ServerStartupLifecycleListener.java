package com.ecg.replyts.core.webapi.util;

import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * listener that awaits the complete startup for all contexts known to the embedded webserver.
 */
public class ServerStartupLifecycleListener extends AbstractLifeCycleListener {

    private CountDownLatch cdl = new CountDownLatch(1);
    private Throwable occuredEx;

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        this.occuredEx = cause;
        cdl.countDown();
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        this.occuredEx = null;
        cdl.countDown();
    }

    /**
     * blocks until all contexts have started up completely or have reported a failure during start up. if a failure occured or the startup did not complete in a reasonable timeframe, an exception will be thrown.
     */
    public void awaitStartup() {
        try {
            if (!cdl.await(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("ReplyTS WebApi Startup Timed out");
            }
        } catch (Exception e) {
            throw new RuntimeException("ReplyTS WebApi Startup Failed", e);
        }
        if (occuredEx != null) {
            throw new RuntimeException("ReplyTS WebApi Startup Failed", occuredEx);
        }

    }
}