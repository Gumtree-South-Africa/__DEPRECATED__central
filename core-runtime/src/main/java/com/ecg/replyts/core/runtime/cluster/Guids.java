package com.ecg.replyts.core.runtime.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.api.util.UnsignedLong;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates globally unique ids, regarding server startup local increment + time + process-id.
 */
public class Guids {

    private final String initialPart;
    private final AtomicLong id;

    private static final Logger LOG = LoggerFactory.getLogger(Guids.class);

    private Guids() {
        this(0, new CurrentClock(), new SystemIdentifier());
    }

    Guids(long initialValue, Clock clock, SystemIdentifier systemIdentifier) {
        this.initialPart = ":" + systemIdentifier.getId() + ":" +  UnsignedLong.fromLong(clock.now().getTime()).toBase30();
        this.id = new AtomicLong(initialValue);
    }

    public static Guids instance() {
        return GuidsHolder.INSTANCE;
    }

    public static String next() {
        return GuidsHolder.INSTANCE.nextGuid();
    }

    String nextGuid() {
        return UnsignedLong.fromLong(id.incrementAndGet()).toBase30() + initialPart;
    }

    static class SystemIdentifier {
        private final String id;

        SystemIdentifier() {
            // something like '<pid>@<hostname>:$NOMAD_ALLOC_ID', at least in SUN / Oracle JVMs
            LOG.info(String.format("JVM Identifier: %s - Nomad Alloc Id: %s", ManagementFactory.getRuntimeMXBean().getName(), System.getenv("NOMAD_ALLOC_ID")));
            String systemName = ManagementFactory.getRuntimeMXBean().getName() + ":" + System.getenv("NOMAD_ALLOC_ID");
            final long unsignedSystemId = Integer.toUnsignedLong(systemName.hashCode());
            this.id = UnsignedLong.fromLong(unsignedSystemId).toBase30();
        }

        // Processor id as base 30 string.
        public String getId() {
            return id;
        }
    }

    private static class GuidsHolder {
        private static final Guids INSTANCE = new Guids();
    }
}
