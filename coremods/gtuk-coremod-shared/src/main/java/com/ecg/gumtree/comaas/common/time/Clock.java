package com.ecg.gumtree.comaas.common.time;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * A clock providing the current time
 */
public interface Clock {

    /**
     * @return the time now in milliseconds
     */
    long now();

    /**
     * @return the time now in milliseconds
     */
    long getMillis();

    /**
     * @return the time now as a standard Java Date() object
     */
    Date getDate();

    /**
     * @return the time now as a JodaTime DateTime() object
     */
    DateTime getDateTime();
}
