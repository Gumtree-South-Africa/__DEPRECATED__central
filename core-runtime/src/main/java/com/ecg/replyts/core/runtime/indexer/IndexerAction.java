package com.ecg.replyts.core.runtime.indexer;

import org.joda.time.DateTime;

public interface IndexerAction {

    void doIndexBetween(DateTime dateFrom, DateTime dateTo, IndexingMode indexingMode, IndexingJournal journal);
}
