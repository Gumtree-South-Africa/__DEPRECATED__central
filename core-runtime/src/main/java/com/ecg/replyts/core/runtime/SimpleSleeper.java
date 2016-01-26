package com.ecg.replyts.core.runtime;

import java.util.concurrent.TimeUnit;

public class SimpleSleeper implements Sleeper {

    @Override
    public void sleep(TimeUnit unit, long timeout) throws InterruptedException {
        unit.sleep(timeout);
    }
}
