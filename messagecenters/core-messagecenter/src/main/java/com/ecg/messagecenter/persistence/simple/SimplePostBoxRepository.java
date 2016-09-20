package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.query.StreamingOperation;
import org.joda.time.DateTime;

import java.util.List;

public interface SimplePostBoxRepository {
    PostBox byId(String email);

    void write(PostBox postBox);

    void write(PostBox postBox, List<String> deletedIds);

    void cleanup(DateTime time);

    long getMessagesCount(DateTime fromDate, DateTime toDate);

    StreamingOperation<IndexEntry> streamPostBoxIds(DateTime fromDate, DateTime toDate);

    List<String> getPostBoxIds(DateTime fromDate, DateTime toDate);

}
