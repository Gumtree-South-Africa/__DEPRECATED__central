package com.ecg.comaas.core.filter.volume;

import com.ecg.comaas.core.filter.volume.Quota;
import com.ecg.comaas.core.filter.volume.VolumeFilterFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class VolumeFilterFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void occurrenceCapIsEnforced() {
        VolumeFilterFactory.checkMaximumPossibleOccurrenceCount(
                Arrays.asList(
                        new Quota(3, 1, TimeUnit.HOURS, 1),
                        new Quota(20, 1, TimeUnit.DAYS, 1)
                ),
                10
        ); // must fail with IEA since (24 hrs / 1 hr) * 3 occurrences per hr = 72 occurrences > 10 allowed occurrences
    }

    @Test
    public void occurrenceHappyPath() {
        VolumeFilterFactory.checkMaximumPossibleOccurrenceCount(
                Arrays.asList(
                        new Quota(3, 1, TimeUnit.HOURS, 1),
                        new Quota(20, 1, TimeUnit.DAYS, 1)
                ),
                72
        );
    }

    @Test
    public void longestQuotaMillisCalculation() {
        assertEquals(
                24 * 60 * 60 * 1000,
                VolumeFilterFactory.getLongestQuotaPeriodMillis(Arrays.asList(
                        new Quota(3, 1, TimeUnit.HOURS, 1),
                        new Quota(20, 1, TimeUnit.DAYS, 1)
                ))
        );
    }
}
