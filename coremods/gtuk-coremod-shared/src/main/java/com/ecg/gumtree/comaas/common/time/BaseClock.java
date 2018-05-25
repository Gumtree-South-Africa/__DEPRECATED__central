package com.ecg.gumtree.comaas.common.time;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Base Clock class that determines class based date values from milliseconds returned by the getMillis method
 */
public abstract class BaseClock implements Clock {

    @Override
    public final long now() {
        return getMillis();
    }

    @Override
    public final Date getDate() {
        return new Date(getMillis());
    }

    @Override
    public final DateTime getDateTime() {
        return new DateTime(getMillis());
    }
}
