package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component("timeoutHeldsCronJobTimeframe")
public class Timeframe {
    @Value("${cronjob.sendHeld.csWorkingHoursStart:0}")
    private int csWorkingHoursStart;

    @Value("${cronjob.sendHeld.csWorkingHoursEnd:24}")
    private int csWorkingHoursEnd;

    @Value("${cronjob.sendHeld.retentionTimeHours:12}")
    private int retentionTime;

    private Clock clock = new CurrentClock();

    private Range<Integer> cronJobExecutionTimerange;

    @PostConstruct
    private void initialize() {
        Preconditions.checkArgument(csWorkingHoursStart >= 0 && csWorkingHoursStart <= 24, "working hours must be in 0...24 range");
        Preconditions.checkArgument(csWorkingHoursEnd >= 0 && csWorkingHoursEnd <= 24, "working hours must be in 0...24 range");
        Preconditions.checkArgument(csWorkingHoursStart < csWorkingHoursEnd, "working hours start must be smaller than end");
        Preconditions.checkArgument(retentionTime < (csWorkingHoursEnd - csWorkingHoursStart), "the retention time in hours must be smaller than the hours CS agents work per day. This is due the bloody algorithm used that is not too smart");

        cronJobExecutionTimerange = Range.closedOpen(csWorkingHoursStart + retentionTime, csWorkingHoursEnd);
    }

    public boolean operateNow() {
        int hourOfDay = LocalDateTime.ofInstant(clock.now().toInstant(), ZoneId.systemDefault()).getHour();

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
