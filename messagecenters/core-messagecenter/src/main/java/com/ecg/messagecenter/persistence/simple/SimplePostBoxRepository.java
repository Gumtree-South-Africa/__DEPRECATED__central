package com.ecg.messagecenter.persistence.simple;

import org.joda.time.DateTime;

import java.util.List;

public interface SimplePostBoxRepository {
    PostBox byId(String email);

    void write(PostBox postBox);

    void write(PostBox postBox, List<String> deletedIds);

    void cleanup(DateTime time);
}
