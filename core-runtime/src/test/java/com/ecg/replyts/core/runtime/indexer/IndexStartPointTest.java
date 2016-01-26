package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.util.Clock;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.joda.time.DateTimeZone.forOffsetHours;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexStartPointTest {

    @Test
    public void calculatesStartPointCorrectly() {
        Clock clock = mock(Clock.class);
        when(clock.now()).thenReturn(new DateTime(2010, 1, 1, 0, 0, 0, 0, forOffsetHours(0)).toDate());
        assertEquals(new DateTime(2009, 12, 31, 0, 0, 0, 0, forOffsetHours(0)), new IndexStartPoint(clock, 1).startTimeForFullIndex().toDateTime(forOffsetHours(0)));
    }
}
