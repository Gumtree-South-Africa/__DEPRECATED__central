package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class EventStreamProcessorTest {

    private final Quota quota = new Quota(10, 10, TimeUnit.MINUTES, 100);
    private EventStreamProcessor esp;

    @Before
    public void setUp() {
        esp = new EventStreamProcessor(Lists.newArrayList(quota));
    }

    @Test
    public void countsSimpleEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", quota), equalTo(3L));
    }

    @Test
    public void countsDiffEvents() {
        esp.mailReceivedFrom("foo@bar.com");
        esp.mailReceivedFrom("bar@foo.com");
        esp.mailReceivedFrom("foo@bar.com");

        assertThat(esp.count("foo@bar.com", quota), equalTo(2L));
        assertThat(esp.count("bar@foo.com", quota), equalTo(1L));
    }

    @Test
    public void unknownUserCountZero() {
        assertThat(esp.count("foo@bar.com", quota), equalTo(0L));
    }



}
