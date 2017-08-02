package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.espertech.esper.client.EPStatement;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EventStreamProcessorTest {

    private final Quota quota = new Quota(10, 10, TimeUnit.MINUTES, 100);
    private EventStreamProcessor esp;

    @Before
    public void setUp() {
        esp = new EventStreamProcessor();
        esp.initialize();
        esp.register("esp-instance", Lists.newArrayList(quota));
    }

    @Test
    public void countsSimpleEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", "esp-instance", quota), equalTo(3L));
    }

    @Test
    public void countsDiffEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("bar@foo.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", "esp-instance", quota), equalTo(2L));
        assertThat(esp.count("bar@foo.com", "esp-instance", quota), equalTo(1L));
    }

    @Test
    public void unknownUserCountZero() {
        assertThat(esp.count("foo@bar.com", "esp-instance", quota), equalTo(0L));
    }

    @Test
    public void testRegister() {
        Quota quota1 = new Quota(10, 10, TimeUnit.MINUTES, 10);
        Quota quota2 = new Quota(10, 10, TimeUnit.MINUTES, 20);
        Quota quota3 = new Quota(20, 20, TimeUnit.MINUTES, 30);

        EventStreamProcessor esp = new EventStreamProcessor();
        esp.initialize();
        esp.register("volumefilter", Arrays.asList(quota1, quota2, quota3, quota1, quota2));

        Map<String, List<EPStatement>> windows = esp.getWindows();
        assertEquals(3, windows.size());

        assertTrue(windows.containsKey("volumevolumefilterquota10minutes10"));
        assertTrue(windows.containsKey("volumevolumefilterquota10minutes20"));
        assertTrue(windows.containsKey("volumevolumefilterquota20minutes30"));

        esp.register("volumefilter", Arrays.asList(quota1, quota2, quota3, quota1, quota2));
        Map<String, List<EPStatement>> windows2 = esp.getWindows();
        assertEquals(3, windows2.size());

        assertTrue(windows2.containsKey("volumevolumefilterquota10minutes10"));
        assertTrue(windows2.containsKey("volumevolumefilterquota10minutes20"));
        assertTrue(windows2.containsKey("volumevolumefilterquota20minutes30"));

        // The second register call does not change the instances in windows map.
        assertSame(windows.containsKey("volumevolumefilterquota10minutes10"), windows2.containsKey("volumevolumefilterquota10minutes10"));
        assertSame(windows.containsKey("volumevolumefilterquota10minutes20"), windows2.containsKey("volumevolumefilterquota10minutes20"));
        assertSame(windows.containsKey("volumevolumefilterquota20minutes30"), windows2.containsKey("volumevolumefilterquota20minutes30"));
    }
}
