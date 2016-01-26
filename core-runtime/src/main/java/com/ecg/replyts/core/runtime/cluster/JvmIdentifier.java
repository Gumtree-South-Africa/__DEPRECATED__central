package com.ecg.replyts.core.runtime.cluster;

import java.lang.management.ManagementFactory;

class JvmIdentifier {

    private final String id;

    JvmIdentifier() {
        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        // Method `toUnsignedString` ignores the sign bit, that's okay here.
        this.id = Integer.toUnsignedString(jvmName.hashCode(), 36);
    }

    // Processor id as base 36 string.
    public String getId() {
        return id;
    }

}
