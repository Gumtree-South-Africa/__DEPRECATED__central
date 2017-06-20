package com.ecg.messagecenter.persistence;

import java.util.Objects;

/**
 * User: maldana
 * Date: 11.02.14
 * Time: 11:03
 *
 * @author maldana@ebay.de
 */
public class Counter {

    private long value;

    public Counter(long value) {
        if (value < 0) {
            this.value = 0;
        } else {
            this.value = value;
        }
    }

    public Counter() {
        this(0);
    }

    public void inc() {
        value++;
    }

    public void dec() {
        dec(1);
    }

    public void dec(long number) {
        if (number > 0) {
            value = Math.max(0, value - number);
        } else {
            throw new IllegalArgumentException("Counter cannot decrement the negative number: " + number);
        }
    }

    public long getValue() {
        return value;
    }

    public void reset() {
        value = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Counter counter = (Counter) o;
        return value == counter.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
