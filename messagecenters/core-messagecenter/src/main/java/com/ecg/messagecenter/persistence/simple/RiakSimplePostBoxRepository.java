package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.query.StreamingOperation;
import org.joda.time.DateTime;

import java.util.List;

public interface RiakSimplePostBoxRepository extends SimplePostBoxRepository {
    long getMessagesCount(DateTime fromDate, DateTime toDate);

    StreamingOperation<IndexEntry> streamPostBoxIds(DateTime fromDate, DateTime toDate);

    List<String> getPostBoxIds(DateTime fromDate, DateTime toDate);
}
