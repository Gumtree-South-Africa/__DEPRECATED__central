package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.codahale.metrics.Timer;

import java.util.Optional;

public interface AnalyticsService<T extends AnalyticsEvent> {

    public void sendAsyncEvent(T event, Optional<Timer> timer);
}
