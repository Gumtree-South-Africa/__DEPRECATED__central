package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.util.UnsignedLong;
import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.api.util.CurrentClock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates globally unique ids, regarding server startup local increment + time + process-id.
 */
public class Guids {

    private final String initialPart;
    private final AtomicLong id;

    public Guids() {
        this(0, new CurrentClock(), new JvmIdentifier());
    }

    Guids(long initialValue, Clock clock, JvmIdentifier jvmIdentifier) {
        this.initialPart = ":" + jvmIdentifier.getId() + ":" + UnsignedLong.fromLong(clock.now().getTime()).toBase30();
        this.id = new AtomicLong(initialValue);
    }

    public String nextGuid() {
        return UnsignedLong.fromLong(id.incrementAndGet()).toBase30() + initialPart;
    }

}
