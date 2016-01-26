package com.ecg.replyts.core.runtime;

import java.util.concurrent.TimeUnit;

/**
 * Sleeps for a defined time. This interface can be mocked in tests. Introduce this interface to keep unit test fast.
 */
public interface Sleeper {

    void sleep(TimeUnit unit, long timeout) throws InterruptedException;

}
