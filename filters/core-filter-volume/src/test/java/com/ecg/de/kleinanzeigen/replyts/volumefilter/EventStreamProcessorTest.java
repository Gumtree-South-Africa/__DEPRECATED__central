package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        esp.register(Collections.singletonList(new Window("esp-instance", quota)));
    }

    @Test
    public void countsSimpleEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", new Window("esp-instance", quota)), equalTo(3L));
    }

    @Test
    public void countsDiffEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("bar@foo.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", new Window("esp-instance", quota)), equalTo(2L));
        assertThat(esp.count("bar@foo.com", new Window("esp-instance", quota)), equalTo(1L));
    }

    @Test
    public void unknownUserCountZero() {
        assertThat(esp.count("foo@bar.com", new Window("esp-instance", quota)), equalTo(0L));
    }

    @Test
    public void testRegister() {
        Quota quota1 = new Quota(10, 10, TimeUnit.MINUTES, 10);
        Quota quota2 = new Quota(10, 10, TimeUnit.MINUTES, 20);
        Quota quota3 = new Quota(20, 20, TimeUnit.MINUTES, 30);

        String instanceId = "volumefilter";

        EventStreamProcessor esp = new EventStreamProcessor();
        esp.initialize();

        List<Window> win1 = Stream.of(quota1, quota2, quota3, quota1, quota2)
                .map(quota -> new Window(instanceId, quota))
                .collect(Collectors.toList());

        esp.register(win1);

        Map<Window, EventStreamProcessor.EsperWindowLifecycle> windows = esp.getWindowsStatements();
        assertEquals(3, windows.size());

        assertTrue(windows.containsKey(new Window(instanceId, quota1)));
        assertTrue(windows.containsKey(new Window(instanceId, quota2)));
        assertTrue(windows.containsKey(new Window(instanceId, quota3)));

        List<Window> win2 = Stream.of(quota1, quota2, quota3, quota1, quota2)
                .map(quota -> new Window(instanceId, quota))
                .collect(Collectors.toList());

        esp.register(win2);
        Map<Window, EventStreamProcessor.EsperWindowLifecycle> windows2 = esp.getWindowsStatements();
        assertEquals(3, windows2.size());

        assertTrue(windows2.containsKey(new Window(instanceId, quota1)));
        assertTrue(windows2.containsKey(new Window(instanceId, quota2)));
        assertTrue(windows2.containsKey(new Window(instanceId, quota1)));

        // The second register call does not change the instances in windows map.
        assertSame(windows.containsKey(new Window(instanceId, quota1)),
                windows2.containsKey(new Window(instanceId, quota1)));
        assertSame(windows.containsKey(new Window(instanceId, quota1)),
                windows2.containsKey(new Window(instanceId, quota1)));
        assertSame(windows.containsKey(new Window(instanceId, quota1)),
                windows2.containsKey(new Window(instanceId, quota1)));
    }

    @Test
    public void testRegisterIgnoreDifferentScore() {
        EventStreamProcessor esp = new EventStreamProcessor();
        esp.initialize();

        Quota quota1 = new Quota(10, 10, TimeUnit.MINUTES, 10);
        Window window1 = new Window("instanceId", quota1);

        Quota quota2 = new Quota(10, 10, TimeUnit.MINUTES, 99999);
        Window window2 = new Window("instanceId", quota2);

        esp.register(Collections.singletonList(window1));
        esp.register(Collections.singletonList(window2));

        assertEquals(1, esp.getWindowsStatements().size());
        assertTrue(esp.getWindowsStatements().containsKey(window1));
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

        List<Window> windows1 = Stream.of(quota1, quota2, quota3)
                .map(quota -> new Window(instanceId, quota))
                .collect(Collectors.toList());

        esp.register(windows1);

        List<Window> windows2 = Stream.of(quota4, quota5, quota6)
                .map(quota -> new Window(instanceId2, quota))
                .collect(Collectors.toList());

        esp.register(windows2);

        Map<Window, EventStreamProcessor.EsperWindowLifecycle> win = esp.getWindowsStatements();
        assertEquals(6, win.size());

        esp.unregister(instanceId);

        Map<Window, EventStreamProcessor.EsperWindowLifecycle> win1 = esp.getWindowsStatements();
        assertEquals(3, win1.size());

        boolean onlyInstance2windows = win1.keySet().stream()
                .allMatch(window -> instanceId2.equals(window.getInstanceId()));
        assertTrue(onlyInstance2windows);
    }
}
