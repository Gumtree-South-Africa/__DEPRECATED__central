package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.util.Clock;
import org.joda.time.DateTime;

class IndexStartPoint {
    private final Clock clock;
    private final int maxAgeDays;

    IndexStartPoint(Clock clock, int maxAgeDays) {
        this.clock = clock;
        this.maxAgeDays = maxAgeDays;
    }

    DateTime startTimeForFullIndex() {
        return new DateTime(clock.now()).minusDays(maxAgeDays);
    }
}
