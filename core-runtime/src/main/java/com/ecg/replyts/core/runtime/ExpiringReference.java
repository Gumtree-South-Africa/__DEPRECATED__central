package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.api.util.CurrentClock;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds a value for a specifiable amount of time. Once that time is over the value is "forgotten" and a configurable default value
 * (or null) is returned.
 *
 * @author mhuttar
 */
public final class ExpiringReference<T> {


    private class ExpiringValue {
        private final T value;
        private final long validTo;
        private final long setAt;

        ExpiringValue(T value, long validTo, long setAt) {
            this.value = value;
            this.validTo = validTo;
            this.setAt = setAt;
        }

    }

    private final T fallbackValue;
    private final Clock clock;
    private final long referenceValidForMillis;

    private final AtomicReference<ExpiringValue> value;

    ExpiringReference(T fallbackValue, long referenceValidForMillis, Clock clock) {

        this.fallbackValue = fallbackValue;
        this.clock = clock;
        value = new AtomicReference<>(new ExpiringValue(fallbackValue, 0L, clock.now().getTime()));
        this.referenceValidForMillis = referenceValidForMillis;
    }

    public void set(T value) {
        long now = clock.now().getTime();
        this.value.set(new ExpiringValue(value, now + referenceValidForMillis, now));
    }

    public ExpiringReference<T> afterwards(T fallbackValue) {
        return new ExpiringReference<>(fallbackValue, referenceValidForMillis, clock);
    }

    public static <T> ExpiringReference<T> validFor(long value, TimeUnit tu) {
        return new ExpiringReference<>(null, tu.toMillis(value), new CurrentClock());
    }

    public String report() {
        ExpiringValue val = value.get();

        double setSecsAgo = ((double) (clock.now().getTime() - val.setAt)) / 1000.0d;
        double expiresInSecs = ((double) (val.validTo - clock.now().getTime())) / 1000.0d;

        if (val.validTo < clock.now().getTime()) {

            return String.format(Locale.ENGLISH, "Expired, Now: %s (before: %s; set %.3f secs ago, expired %.3f secs ago)",
                    fallbackValue, val.value, setSecsAgo, -expiresInSecs);
        }
        return String.format(Locale.ENGLISH, "Valid: %s (set %.3f secs ago, expires in %.3f secs)", val.value, setSecsAgo, expiresInSecs);
    }


    public T get() {
        ExpiringValue val = this.value.get();
        if (val.validTo < clock.now().getTime()) {
            return fallbackValue;
        }
        return val.value;
    }
}
