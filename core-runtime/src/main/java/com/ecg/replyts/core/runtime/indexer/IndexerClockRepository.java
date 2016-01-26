package com.ecg.replyts.core.runtime.indexer;

import org.joda.time.DateTime;

/**
 * Created by pragone
 * Created on 18/10/15 at 6:49 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public interface IndexerClockRepository {
    void set(DateTime lastRun);

    void clear();

    DateTime get();
}
