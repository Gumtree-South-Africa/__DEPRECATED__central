package com.ecg.replyts.integration.riak.persistence;

import com.basho.riak.client.query.indexes.BinIndex;
import com.basho.riak.client.query.indexes.IntIndex;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Bucket {


    private Map<String, Value> contents = Maps.newConcurrentMap();

    public Bucket() {
    }

    public void put(String key, Value val) {
        contents.put(key, val);
    }

    public Value get(String key) {
        return contents.get(key);
    }

    public void delete(String key) {
        contents.remove(key);
    }

    public Iterable<String> keys() {
        return contents.keySet();
    }

    public List<String> fetch2iLong(String index, Range<Long> closed) {
        IntIndex intIdx = IntIndex.named(index);
        List<String> keys = Lists.newArrayList();
        for (Entry<String, Value> e : contents.entrySet()) {
            for (Long lng : e.getValue().get(intIdx)) {
                if (closed.contains(lng)) {
                    keys.add(e.getKey());
                }
            }

        }
        Collections.sort(keys);
        return keys;
    }

    public List<String> fetch2iBin(String index, Range<String> closed) {
        BinIndex binIdx = BinIndex.named(index);
        List<String> keys = Lists.newArrayList();
        for (Entry<String, Value> e : contents.entrySet()) {
            for (String s : e.getValue().get(binIdx)) {
                if (closed.contains(s)) {
                    keys.add(e.getKey());
                }
            }

        }
        Collections.sort(keys);
        return keys;
    }

}
