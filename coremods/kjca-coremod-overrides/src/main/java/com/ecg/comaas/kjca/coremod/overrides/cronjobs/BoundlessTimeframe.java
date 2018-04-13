package com.ecg.comaas.kjca.coremod.overrides.cronjobs;

import com.ecg.replyts.app.cronjobs.Timeframe;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Redefine the working timeframe for the send-out-helds job to be "any time",
// so that the retention period is honored with no regard for CS working hours.

@Component
@Primary
public class BoundlessTimeframe extends Timeframe {
    @Override
    public boolean operateNow() {
        return true;
    }
}
