package com.ecg.comaas.bt.filter.volume;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

class MailAddressMemorizer {



    private volatile BloomFilter<String> filter;
    private volatile long filterExpiration;


    private final long maxFilterAgeMillis;
    private final int expectedInsertions;

    public MailAddressMemorizer(long maxFilterAgeMillis, int expectedInsertions) {
        this.maxFilterAgeMillis = maxFilterAgeMillis;
        this.expectedInsertions = expectedInsertions;
        getFilter();
    }

    private BloomFilter<String> getFilter() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (now < filterExpiration) {
                return filter;
            }
            filter = BloomFilter.create(new Funnel<String>() {
                public void funnel(String from, PrimitiveSink into) {
                    into.putString(from.toLowerCase(), Charsets.UTF_8);
                }
            }, expectedInsertions, 0.00001);
            filterExpiration = now + maxFilterAgeMillis;
            return filter;
        }

    }


    public boolean couldBeSeenAlready(String mailAddress) {
        return getFilter().mightContain(mailAddress);
    }

    public void mark(String s) {
        getFilter().put(s);
    }
}