package com.ecg.de.ebayk.messagecenter.persistence;

/**
 * User: maldana
 * Date: 11.02.14
 * Time: 11:03
 *
 * @author maldana@ebay.de
 */
public class Counter {

    private Long value;

    public Counter(Long value) {
        if (value < 0) {
            this.value = 0L;
        } else {
            this.value = value;
        }
    }

    public Counter() {
        this(0L);
    }

    public void inc() {
        value++;
    }

    public void dec() {
        // counters are always positive
        if (value > 0) {
            value--;
        }
    }

    public Long getValue() {
        return value;
    }

    public void reset() {
        value = 0L;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Counter counter = (Counter) o;

        if (value != null ? !value.equals(counter.value) : counter.value != null)
            return false;

        return true;
    }

    @Override public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
