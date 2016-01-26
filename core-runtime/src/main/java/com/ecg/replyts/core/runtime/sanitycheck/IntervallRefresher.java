package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.runtime.sanitycheck.adapter.CheckAdapter;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;


class IntervallRefresher {

    private static final Logger LOG = LoggerFactory.getLogger(IntervallRefresher.class);


    private final Timer timer = new Timer("Sanity-Check Intervall Refresher Timer", true);
    private final CheckAdapter checkAdapter;
    private final long interval;

    public IntervallRefresher(CheckAdapter check) {
        this(check, 60);
    }

    IntervallRefresher(CheckAdapter check, long intervalSeconds) {
        this.checkAdapter = check;
        Preconditions.checkArgument(intervalSeconds > 0);
        this.interval = intervalSeconds;

    }

    /**
     * Starts with the periodically checking.
     */
    public void start() {
        LOG.info(format("The sanity check interval is %d seconds.", interval));

        long intervalInMilliseconds = TimeUnit.MILLISECONDS.convert(interval, TimeUnit.SECONDS);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkAdapter.execute();
            }
        }, intervalInMilliseconds, intervalInMilliseconds);

    }

    /**
     * Stops the checking.
     */
    public void stop() {
        timer.cancel();
    }

    public long getInterval() {
        return interval;
    }

}
