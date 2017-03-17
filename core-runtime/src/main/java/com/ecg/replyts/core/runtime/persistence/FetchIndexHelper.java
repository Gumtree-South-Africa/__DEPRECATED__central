package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.indexes.FetchIndex;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;

import java.util.List;

import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

public abstract class FetchIndexHelper {
    public static List<IndexEntry> fetchResult(FetchIndex<Number> fetchIndex, DateTime time, int maxResults) throws RiakException {
        Preconditions.checkNotNull(fetchIndex, time);
        // FIXME: The getAll() call put all results into one list. When the amount of data is quite big, it might be possible that we run out of memory!
        // Instead of getAll(), the iterator should be used!
        return fetchIndex
                .from(0)
                .maxResults(maxResults) // Can only be used with executeStreaming! Actually, maxResults means the page size the Riak client will load.
                .to(timestampInMinutes(time))
                .executeStreaming() // Need for max results
                .getAll(); // fetch all results and close connections automaticly (by calling last hasNext())
    }

    public static final int MIN_LOG_INTERVAL = 100;

    public static int logInterval(int maxResults) {
        return Math.min(Math.max(maxResults / 10, MIN_LOG_INTERVAL), 10000);
    }
}
