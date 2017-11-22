package com.ecg.replyts.core.runtime.indexer;

import org.joda.time.DateTime;

public interface IndexerClockRepository {
    void set(DateTime lastRun);

    void clear();

    DateTime get();
}
