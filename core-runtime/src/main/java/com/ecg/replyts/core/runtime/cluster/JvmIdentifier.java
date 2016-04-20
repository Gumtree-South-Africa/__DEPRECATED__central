package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.util.UnsignedLong;

import java.lang.management.ManagementFactory;

class JvmIdentifier {

    private final String id;

    JvmIdentifier() {
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final long unsignedJvmId = Integer.toUnsignedLong(jvmName.hashCode());
        this.id = UnsignedLong.fromLong(unsignedJvmId).toBase30();
    }

    // Processor id as base 30 string.
    public String getId() {
        return id;
    }

}
