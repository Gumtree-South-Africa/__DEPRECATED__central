package com.ecg.replyts.integration.riak.persistence;

import com.basho.riak.client.query.indexes.BinIndex;
import com.basho.riak.client.query.indexes.IntIndex;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;

public class Value {
    private final Map<BinIndex, Set<String>> binIndexes;
    private final Map<IntIndex, Set<Long>> intIndexes;
    private final Map<String, String> properties;
    private final byte[] value;

    public Value(byte[] value, String contentType, Map<BinIndex, Set<String>> binIndexes, Map<IntIndex, Set<Long>> intIndexes) {
        this.value = Arrays.copyOf(value, value.length);

        this.properties = ImmutableMap.of(
                "Content-Type", contentType,
                "X-Riak-Vclock", String.valueOf(System.currentTimeMillis()),
                "Last-Modified", new GregorianCalendar().toString());

        this.binIndexes = binIndexes;
        this.intIndexes = intIndexes;
    }


    public Map<String, String> getProperties() {
        return properties;
    }

    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    public byte[] getVClock() {
        return properties.get("X-Riak-Vclock").getBytes();
    }

    public Set<Long> get(IntIndex i) {
        if (intIndexes.containsKey(i)) {
            return intIndexes.get(i);
        }
        return Sets.newHashSet();
    }



    public Set<String> get(BinIndex i) {
        if (binIndexes.containsKey(i)) {
            return binIndexes.get(i);
        }
        return Sets.newHashSet();
    }
}
