package com.ecg.comaas.core.filter.volume.registry;

import java.util.Date;

public interface OccurrenceRegistry {
    void register(String userId, String messageId, Date receivedTime);

    int count(String userId, Date fromTime);
}
