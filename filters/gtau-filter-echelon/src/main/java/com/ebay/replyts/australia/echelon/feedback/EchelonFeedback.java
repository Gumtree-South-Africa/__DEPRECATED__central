package com.ebay.replyts.australia.echelon.feedback;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;

/**
 * @author mdarapour
 */
public class EchelonFeedback extends FilterFeedback {
    public EchelonFeedback(String message, int score) {
        super("Echelon", message, score, FilterResultState.DROPPED);
    }
}
