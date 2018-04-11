package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;

public class EchelonFeedback extends FilterFeedback {
    public EchelonFeedback(String message, int score) {
        super("Echelon", message, score, FilterResultState.DROPPED);
    }
}
