package com.ecg.de.kleinanzeigen.replyts.volumefilter.registry;

import java.util.Date;

public interface OccurrenceRegistry {
    void register(String userId, String messageId, Date receivedTime);

    int count(String userId, Date fromTime);
}
