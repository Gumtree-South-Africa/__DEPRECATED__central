package com.ecg.de.kleinanzeigen.ruleperformance;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

/**
* @author mhuttar
*/
enum PerformanceLogType {
    FIRE,
    CONFIRMED,
    MISFIRE;

    public static Optional<PerformanceLogType> valueOf(Message msg) {
        switch (msg.getHumanResultState()) {
            case BAD: return of(CONFIRMED);
            case GOOD:return of(MISFIRE);
            case UNCHECKED: return of(FIRE);
        }
        return absent();
    }
}
