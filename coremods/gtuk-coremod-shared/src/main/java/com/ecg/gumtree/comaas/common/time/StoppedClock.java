package com.ecg.gumtree.comaas.common.time;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Clock that doesn't change over time, useful for testing
 */
public final class StoppedClock extends BaseClock {

    private long milliseconds;

    /**
     * ctor
     */
    public StoppedClock() {
        this(System.currentTimeMillis());
    }

    /**
     * ctor
     * @param milliseconds value clock should represent
     */
    public StoppedClock(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * ctor
     * @param date value clock should represent
     */
    public StoppedClock(Date date) {
        this(date.getTime());
    }

    /**
     * ctor
     * @param dateTime value clock should represent
     */
    public StoppedClock(DateTime dateTime) {
        this(dateTime.getMillis());
    }

    @Override
    public long getMillis() {
        return milliseconds;
    }

    /**
     * @param value milliseconds to set clock to
     */
    public void setMillis(long value) {
        milliseconds = value;
    }

    /**
     * @param value Date to set clock to
     */
    public void setDate(Date value) {
        setMillis(value.getTime());
    }

    /**
     * @param value DateTime to set clock to
     */
    public void setDateTime(DateTime value) {
        setMillis(value.getMillis());
    }
}
