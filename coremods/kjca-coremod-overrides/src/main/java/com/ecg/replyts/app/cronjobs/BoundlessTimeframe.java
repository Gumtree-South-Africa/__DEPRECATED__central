package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.util.Clock;

// Redefine the working timeframe for the send-out-helds job to be "any time",
// so that the retention period is honored with no regard for CS working hours.
public class BoundlessTimeframe extends Timeframe {
    public BoundlessTimeframe(int csWorkingHoursStart, int csWorkingHoursEnd, int retentionTime, Clock clock) {
        super(csWorkingHoursStart, csWorkingHoursEnd, retentionTime, clock);
    }

    @Override
    public boolean operateNow() {
        return true;
    }
}
