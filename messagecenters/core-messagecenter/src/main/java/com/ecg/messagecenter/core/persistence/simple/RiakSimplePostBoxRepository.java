package com.ecg.messagecenter.core.persistence.simple;

import org.joda.time.DateTime;

import java.util.List;
import java.util.stream.Stream;

public interface RiakSimplePostBoxRepository extends SimplePostBoxRepository {
    long getMessagesCount(DateTime fromDate, DateTime toDate);

    Stream<String> streamPostBoxIds(DateTime fromDate, DateTime toDate);

    List<String> getPostBoxIds(DateTime fromDate, DateTime toDate);
}
