package com.ecg.replyts.integration.riak.persistence;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

public class Storage {


    private Map<String, Bucket> buckets = Maps.newConcurrentMap();

    public Bucket getBucket(String bucketName) {
        synchronized (this) {
            if (!buckets.containsKey(bucketName)) {
                buckets.put(bucketName, new Bucket());
            }
        }
        return buckets.get(bucketName);
    }

    public Set<String> buckets() {
        return buckets.keySet();
    }

    public void deleteAll() {
        buckets.clear();
    }
}
