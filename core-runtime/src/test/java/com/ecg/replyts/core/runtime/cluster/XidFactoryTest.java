package com.ecg.replyts.core.runtime.cluster;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class XidFactoryTest {

    @Test
    public void nextXid_whenUsedMultithreaded_shouldBeUnique() throws InterruptedException {
        Set<String> xids = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    xids.add(XidFactory.nextXid());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
        assertThat(xids).hasSize(1000);
    }

    @Test
    public void nextXid_shouldBeSortable() {
        int failures = 0;
        String xid = XidFactory.nextXid();
        for (int i = 0; i < 100000; i++) {
            String next = XidFactory.nextXid();
            // This test can occasionally fail, once in 24bits runs due to the internal counter rolling over
            if (next.compareTo(xid) <= 0) {
                failures++;
            }
            xid = next;
        }
        assertThat(failures).isIn(0, 1);
    }

    @Test
    public void nextXid_whenDecoded_shouldBeTwelveBytes()
    {
        String xid = XidFactory.nextXid();
        assertThat(xid).hasSize(20);
        byte[] decode = BaseEncoding.base32Hex().lowerCase().decode(xid);
        assertThat(decode).hasSize(12);
    }
}
