package com.ecg.de.kleinanzeigen.ruleperformance;

import com.ecg.replyts.core.api.model.conversation.Message;

import java.util.Optional;

/**
* @author mhuttar
*/
enum PerformanceLogType {
    FIRE,
    CONFIRMED,
    MISFIRE;

    public static Optional<PerformanceLogType> valueOf(Message msg) {
        switch (msg.getHumanResultState()) {
            case BAD: return Optional.of(CONFIRMED);
            case GOOD:return Optional.of(MISFIRE);
            case UNCHECKED: return Optional.of(FIRE);
        }
        return Optional.empty();
    }
}
