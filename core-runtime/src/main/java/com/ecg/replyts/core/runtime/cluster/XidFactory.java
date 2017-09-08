package com.ecg.replyts.core.runtime.cluster;

import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Globally Unique ID Generator
 *
 * Based on https://github.com/rs/xid
 */
public final class XidFactory {
    private static final BaseEncoding ENCODER = BaseEncoding.base32Hex().omitPadding().lowerCase();
    private static final byte[] TEMPLATE = new byte[12];
    private static final ThreadLocal<byte[]> CACHE = ThreadLocal.withInitial(TEMPLATE::clone);
    private static final Clock CLOCK = Clock.systemUTC();
    private static final AtomicInteger COUNTER = new AtomicInteger(new Random().nextInt());

    private XidFactory() {
        throw new AssertionError();
    }

    static {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        List<String> pidAndHostname = Splitter.on('@').splitToList(name);

        String hostName = pidAndHostname.get(1);
        byte[] machineId = getMd5Digest().digest(hostName.getBytes());
        TEMPLATE[4] = machineId[0];
        TEMPLATE[5] = machineId[1];
        TEMPLATE[6] = machineId[2];

        int pid = Integer.parseInt(pidAndHostname.get(0));
        TEMPLATE[7] = (byte) (pid >> 8);
        TEMPLATE[8] = (byte) (pid);
    }

    public static String nextXid() {
        byte[] xid = CACHE.get();

        long epochSeconds = CLOCK.instant().getEpochSecond();
        xid[0] = (byte) (epochSeconds >> 24);
        xid[1] = (byte) (epochSeconds >> 16);
        xid[2] = (byte) (epochSeconds >> 8);
        xid[3] = (byte) (epochSeconds);

        int count = COUNTER.getAndIncrement();
        xid[9] = (byte) (count >> 16);
        xid[10] = (byte) (count >> 8);
        xid[11] = (byte) (count);

        return ENCODER.encode(xid);
    }

    private static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
