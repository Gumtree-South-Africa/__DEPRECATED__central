package com.ecg.replyts.core.runtime.cron;

/**
 * MBean that is exported for each available cron job that allows triggering this job abnorally via JMX.
 *
 * @author mhuttar
 */
public interface CronJobInvokerMBean {

    /**
     * Invokes it's associated cron job asynchronousely and immediately returns
     */
    void invoke();

}