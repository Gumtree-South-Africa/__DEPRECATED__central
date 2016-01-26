package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.ClusterMonitor;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Timer;
import java.util.TimerTask;

import static com.ecg.replyts.core.api.sanitychecks.Message.fromException;
import static com.ecg.replyts.core.api.sanitychecks.Message.shortInfo;
import static com.ecg.replyts.core.api.sanitychecks.Status.OK;
import static com.ecg.replyts.core.api.sanitychecks.Status.WARNING;

public class RiakClusterMonitor implements ClusterMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakClusterMonitor.class);

    private final long checkIntervalMillis;
    private final Check check;
    private final Timer checkTimer;

    // different thread writing and reading this value: volatile
    // Initialize result with status ok to prevent switching in blockmode on startup.
    private volatile Result result = Result.createResult(OK, shortInfo("no result available"));

    public RiakClusterMonitor(long checkIntervalMillis, Check check) {
        this(checkIntervalMillis, check, new Timer());
    }

    // for test
    RiakClusterMonitor(long checkIntervalMillis, Check check, Timer timer) {
        this.checkIntervalMillis = checkIntervalMillis;
        this.check = check;
        this.checkTimer = timer;
    }

    @PostConstruct
    void init() {
        LOGGER.debug("Scheduling fixed-delay checks");
        checkTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    check();
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    LOGGER.error("Error on check execution", ex);
                    RiakClusterMonitor.this.result = Result.createResult(WARNING, fromException(ex));
                }
            }
        }, 0L, checkIntervalMillis);
    }

    @PreDestroy
    void shutdown() {
        checkTimer.cancel();
    }

    private void check() throws Exception {
        result = check.execute();
    }

    @Override
    public boolean allDatacentersAvailable() {
        return result.status() == OK;
    }

    @Override
    public String report() {
        return result.value().toString();
    }

}
