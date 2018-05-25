package com.ecg.gumtree.comaas.common.time;

/**
 * Clock representing time as determined by the underlying OS
 */
public class SystemClock extends BaseClock {
    @Override
    public final long getMillis() {
        return System.currentTimeMillis();
    }
}
