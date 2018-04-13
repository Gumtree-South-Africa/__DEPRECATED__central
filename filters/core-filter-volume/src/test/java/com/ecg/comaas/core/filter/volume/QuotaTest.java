package com.ecg.comaas.core.filter.volume;

import com.ecg.comaas.core.filter.volume.Quota;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class QuotaTest {

    @Test
    public void naturallyOrdersToScoreDescending() throws Exception {
        List<Quota> quotas = Arrays.asList(
                new Quota(100, 1, TimeUnit.HOURS, 20),
                new Quota(500, 7, TimeUnit.HOURS, 80),
                new Quota(200, 3, TimeUnit.HOURS, 40));

        Collections.sort(quotas);


        assertEquals(Arrays.asList(
                new Quota(500, 7, TimeUnit.HOURS, 80),
                new Quota(200, 3, TimeUnit.HOURS, 40),
                new Quota(100, 1, TimeUnit.HOURS, 20))
                , quotas);

    }
}
