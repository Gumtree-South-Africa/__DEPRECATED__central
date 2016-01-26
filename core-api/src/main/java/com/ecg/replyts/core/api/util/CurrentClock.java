package com.ecg.replyts.core.api.util;

import java.util.Date;

/**
 * Default clock implementation. always returns the operating system's current time.
 */
public class CurrentClock implements Clock {

    @Override
    public Date now() {
        return new Date();
    }

}
