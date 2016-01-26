package com.ecg.replyts.core.api.util;

import java.util.Date;

/**
 * Abstract source for the current time.
 *
 * @author alex
 */
public interface Clock {
    Date now();
}
