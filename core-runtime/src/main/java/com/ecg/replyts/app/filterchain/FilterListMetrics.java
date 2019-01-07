package com.ecg.replyts.app.filterchain;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.configadmin.ConfigurationLabel;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.stereotype.Component;


/**
 * Is a wrapper only to be able to mock the ugly static calls of TimingReports. Otherwise test will getting quite
 * complex.
 *
 * Would be nice if timing reports where not designd in a static way.
 */
@Component
public class FilterListMetrics {
    public Timer.Context newOrExistingTimerFor(ConfigurationLabel filterId) {
        return TimingReports.newOrExistingTimerFor(filterId).time();
    }
}
