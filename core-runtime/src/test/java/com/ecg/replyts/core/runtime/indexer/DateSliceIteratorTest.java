package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.runtime.DateSliceIterator.IterationDirection.PAST_TO_PRESENT;
import static org.junit.Assert.*;


public class DateSliceIteratorTest {
    private static final TimeUnit UNIT = TimeUnit.HOURS;

    @Test
    public void zeroSlices() throws Exception {
        DateTime sameDate = new DateTime();

        Range<DateTime> totalTimeRange = Range.closed(sameDate, sameDate);
        DateSliceIterator dateSliceIterator = new DateSliceIterator(totalTimeRange, 1, UNIT, PAST_TO_PRESENT);

        assertFalse(dateSliceIterator.iterator().hasNext());
    }

    @Test
    public void singleSliceForFullSliceRange() throws Exception {
        DateTime start = new DateTime(0);
        DateTime end = new DateTime(TimeUnit.HOURS.toMillis(1));


        Range<DateTime> totalTimeRange = Range.closed(start, end);
        Iterator<Range<DateTime>> it = new DateSliceIterator(totalTimeRange, 1, UNIT, PAST_TO_PRESENT).iterator();

        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void singleSliceForHalfSliceRange() throws Exception {
        DateTime start = new DateTime(0);
        DateTime end = new DateTime(TimeUnit.MINUTES.toMillis(30));


        Range<DateTime> totalTimeRange = Range.closed(start, end);
        Iterator<Range<DateTime>> it = new DateSliceIterator(totalTimeRange, 1, UNIT, PAST_TO_PRESENT).iterator();

        assertTrue(it.hasNext());
        assertEquals(end, it.next().upperEndpoint());

        assertFalse(it.hasNext());
    }


    @Test
    public void sliceRangeInMultipleParts() throws Exception {
        DateTime start = new DateTime(0);
        DateTime end = new DateTime(TimeUnit.HOURS.toMillis(10));


        Range<DateTime> totalTimeRange = Range.closed(start, end);
        Iterator<Range<DateTime>> it = new DateSliceIterator(totalTimeRange, 1, UNIT, PAST_TO_PRESENT).iterator();

        for (int i = 0; i < 10; i++) {
            assertTrue(it.hasNext());
            it.next();
        }

        assertFalse(it.hasNext());
    }

    @Test
    public void slicesIntoCorrectRanges() throws Exception {
        DateTime start = new DateTime(0);
        DateTime end = new DateTime(TimeUnit.MINUTES.toMillis(90));

        Range<DateTime> totalTimeRange = Range.closed(start, end);
        Iterator<Range<DateTime>> it = new DateSliceIterator(totalTimeRange, 1, UNIT, PAST_TO_PRESENT).iterator();


        assertEquals(Range.closed(start, start.plusHours(1)), it.next());
        assertEquals(Range.closed(start.plusHours(1), start.plusMinutes(90)), it.next());
    }

    @Test
    public void slicesIntoCorrectRanges2() throws Exception {
        DateTime start = new DateTime(0);
        DateTime end = new DateTime(TimeUnit.DAYS.toMillis(100));

        Range<DateTime> totalTimeRange = Range.closed(start, end);
        Iterator<Range<DateTime>> it = new DateSliceIterator(totalTimeRange, 50, TimeUnit.DAYS, PAST_TO_PRESENT).iterator();

        ArrayList<Range<DateTime>> list = Lists.newArrayList(it);
        assertEquals(2, list.size());
    }


}
