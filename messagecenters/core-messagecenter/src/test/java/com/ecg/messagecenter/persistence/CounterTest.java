package com.ecg.messagecenter.persistence;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: maldana
 * Date: 12.02.14
 * Time: 11:43
 *
 * @author maldana@ebay.de
 */
public class CounterTest {

    @Test
    public void zeroLowestValue() {
        assertTrue(0L == new Counter(-1L).getValue());
        assertTrue(0L == new Counter(0L).getValue());
        assertTrue(0L == new Counter().getValue());

        Counter counter = new Counter(0L);
        counter.dec();
        assertTrue(0L == counter.getValue());
    }

    @Test
    public void increment() {
        Counter counter = new Counter();
        counter.inc();
        assertTrue(1L == counter.getValue());
    }

}
