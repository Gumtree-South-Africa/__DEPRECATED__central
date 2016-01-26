package com.ecg.replyts.core.runtime.cron;

import com.ecg.replyts.core.api.cron.CronJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MBean that allows manually triggered launch of Cron Jobs. This one invokes the cron job asynchronousely. That is
 *
 * @author huttar
 */
class CronJobInvoker implements CronJobInvokerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(CronJobInvoker.class);

    private final Class<CronJobExecutor> target;
    private final CronJobService service;

    /**
     * creates a new Launcher MBean that is associated to a cron job
     */
    @SuppressWarnings("unchecked")
    protected CronJobInvoker(CronJobExecutor target, CronJobService service) {
        this.target = (Class<CronJobExecutor>) target.getClass();
        this.service = service;
    }


    @Override
    public void invoke() {
        new Thread() {
            @Override
            public void run() {
                LOG.info("Externally invoked Cron Job {}", target);
                service.invokeMonitored(target);
            }
        }.start();
    }
}