package com.ecg.replyts.core.runtime.cluster;

import com.datastax.driver.core.utils.UUIDs;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Threads;

import java.util.UUID;

@Fork(1)
@Threads(100)
public class XidFactoryBenchmark {

    @Benchmark
    public String xidFactory() {
        return XidFactory.nextXid();
    }

    @Benchmark
    public UUID uuid() {
        return UUID.randomUUID();
    }

    @Benchmark
    public String uuid5() {
        return UUIDs.timeBased().toString();
    }

    @Benchmark
    public String guids() {
        return Guids.next();
    }
}
