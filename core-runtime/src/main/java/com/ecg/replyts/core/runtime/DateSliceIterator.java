package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * iterate over date slices beginning with the start date until the given end date. the length of the slices can be
 * configured
 */
public class DateSliceIterator implements Iterable<Range<DateTime>> {

    private List<Range<DateTime>> slices;

    public int chunkCount() {
        return slices.size();
    }

    public enum IterationDirection {
        PAST_TO_PRESENT, PRESENT_TO_PAST
    }

    public DateSliceIterator(Range<DateTime> totalTimeRange, long sliceValue, TimeUnit sliceValueUnit, IterationDirection iterationDirection) {
        this(totalTimeRange, sliceValue, sliceValueUnit, iterationDirection, null);
    }

    public DateSliceIterator(Range<DateTime> totalTimeRange, long sliceValue,
                             TimeUnit sliceValueUnit, IterationDirection iterationDirection, DateTimeZone timeZone) {

        List<Range<DateTime>> items = Lists.newArrayList();
        DateTime totalTimeRangeWithTZ = totalTimeRange.upperEndpoint();
        DateTime currentOffset = totalTimeRange.lowerEndpoint();
        DateTime currentEnd = totalTimeRange.lowerEndpoint();

        if (timeZone != null) {
            totalTimeRangeWithTZ = totalTimeRangeWithTZ.toDateTime(timeZone);
            currentOffset = currentOffset.toDateTime(timeZone);
            currentEnd = currentEnd.toDateTime(timeZone);
        }
        long sliceSizeMs = sliceValueUnit.toMillis(sliceValue);

        while (currentEnd.isBefore(totalTimeRangeWithTZ)) {
            if (timeZone != null) {
                currentEnd = new DateTime(currentOffset.getMillis() + sliceSizeMs, timeZone);
            } else {
                currentEnd = new DateTime(currentOffset.getMillis() + sliceSizeMs);
            }
            if (currentEnd.isAfter(totalTimeRangeWithTZ))
                currentEnd = totalTimeRangeWithTZ;

            items.add(Range.closed(currentOffset, currentEnd));
            currentOffset = currentEnd;
        }

        if (iterationDirection == IterationDirection.PRESENT_TO_PAST) {
            Collections.reverse(items);
        }
        slices = ImmutableList.copyOf(items);
    }

    @Override
    public Iterator<Range<DateTime>> iterator() {
        return slices.iterator();
    }
}