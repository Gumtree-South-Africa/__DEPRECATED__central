package com.ecg.replyts.core.api.indexer;


import org.joda.time.DateTime;

import java.util.List;

public interface Indexer {

    void fullIndex();

    void deltaIndex();

    void indexSince(DateTime since);

    List<IndexerStatus> getStatus();

}