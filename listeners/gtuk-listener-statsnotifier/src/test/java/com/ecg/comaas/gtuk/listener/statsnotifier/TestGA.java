package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.ecg.comaas.gtuk.listener.statsnotifier.event.GAEvent;

public class TestGA extends GAEvent {

    private TestGA(GAEventBuilder builder) {
        super(builder);
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder extends GAEventBuilder<TestGA> {

        @Override
        public TestGA build() {
            return new TestGA(this);
        }
    }
}
