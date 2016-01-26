package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.util.Clock;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import org.joda.time.DateTime;

public class Timeframe {

    private final Range<Integer> cronJobExecutionTimerange;
    private final int csWorkingHoursStart;
    private final int csWorkingHoursEnd;
    private final int retentionTime;
    private final Clock clock;

    public Timeframe(int csWorkingHoursStart, int csWorkingHoursEnd, int retentionTime, Clock clock) {
        this.csWorkingHoursStart = csWorkingHoursStart;
        this.csWorkingHoursEnd = csWorkingHoursEnd;
        this.retentionTime = retentionTime;
        this.clock = clock;
        Preconditions.checkArgument(csWorkingHoursStart >= 0 && csWorkingHoursStart <= 24, "working hours must be in 0...24 range");
        Preconditions.checkArgument(csWorkingHoursEnd >= 0 && csWorkingHoursEnd <= 24, "working hours must be in 0...24 range");
        Preconditions.checkArgument(csWorkingHoursStart < csWorkingHoursEnd, "workoung hours start must be smaller than end");
        Preconditions.checkArgument(retentionTime < (csWorkingHoursEnd - csWorkingHoursStart), "the retention time in hours must be smaller than the hours CS agents work per day. This is due the bloody algorithm used that is not too smart");
        cronJobExecutionTimerange = Range.closedOpen(csWorkingHoursStart + retentionTime, csWorkingHoursEnd);
    }

    public boolean operateNow() {
        int hourOfDay = new DateTime(clock.now().getTime()).getHourOfDay();
        return cronJobExecutionTimerange.contains(hourOfDay);
    }

    public int getCsWorkingHoursStart() {
        return csWorkingHoursStart;
    }

    public int getCsWorkingHoursEnd() {
        return csWorkingHoursEnd;
    }

    public int getRetentionTime() {
        return retentionTime;
    }
}
