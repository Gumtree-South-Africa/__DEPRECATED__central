package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.espertech.esper.client.EPStatement;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

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

        String instanceId = "volumefilter";

        EventStreamProcessor esp = new EventStreamProcessor();
        esp.initialize();
        esp.register(instanceId, Arrays.asList(quota1, quota2, quota3, quota1, quota2));

        Map<EventStreamProcessor.Window, EPStatement> windows = esp.getWindows();
        assertEquals(3, windows.size());

        assertTrue(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));
        assertTrue(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota2)));
        assertTrue(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota3)));

        esp.register(instanceId, Arrays.asList(quota1, quota2, quota3, quota1, quota2));
        Map<EventStreamProcessor.Window, EPStatement> windows2 = esp.getWindows();
        assertEquals(3, windows2.size());

        assertTrue(windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));
        assertTrue(windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota2)));
        assertTrue(windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));

        // The second register call does not change the instances in windows map.
        assertSame(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota1)),
                windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));
        assertSame(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota1)),
                windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));
        assertSame(windows.containsKey(new EventStreamProcessor.Window(instanceId, quota1)),
                windows2.containsKey(new EventStreamProcessor.Window(instanceId, quota1)));
    }

    @Test
    public void testUnregister() {
        Quota quota1 = new Quota(10, 10, TimeUnit.MINUTES, 10);
        Quota quota2 = new Quota(10, 10, TimeUnit.MINUTES, 20);
        Quota quota3 = new Quota(20, 20, TimeUnit.MINUTES, 30);

        String instanceId = "volumefilter-1";

        Quota quota4 = new Quota(50, 30, TimeUnit.MINUTES, 10);
        Quota quota5 = new Quota(60, 10, TimeUnit.DAYS, 30);
        Quota quota6 = new Quota(70, 20, TimeUnit.DAYS, 30);

        String instanceId2 = "volumefilter-2";

        EventStreamProcessor esp = new EventStreamProcessor();
        esp.initialize();
        esp.register(instanceId, Arrays.asList(quota1, quota2, quota3));
        esp.register(instanceId2, Arrays.asList(quota4, quota5, quota6));

        Map<EventStreamProcessor.Window, EPStatement> windows = esp.getWindows();
        assertEquals(6, windows.size());

        esp.unregister(instanceId);

        Map<EventStreamProcessor.Window, EPStatement> windows1 = esp.getWindows();
        assertEquals(3, windows1.size());

        boolean onlyInstance2windows = windows1.keySet().stream()
                .allMatch(window -> instanceId2.equals(window.getInstanceId()));
        assertTrue(onlyInstance2windows);
    }
}
