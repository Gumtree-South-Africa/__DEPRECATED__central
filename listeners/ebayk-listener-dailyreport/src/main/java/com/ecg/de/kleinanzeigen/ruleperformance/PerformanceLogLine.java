package com.ecg.de.kleinanzeigen.ruleperformance;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;

/**
 * @author mhuttar
 */
class PerformanceLogLine {

    public static final char SEPERATOR = '\t';

    private static final CharMatcher LINEBREAK_REPLACER = CharMatcher.anyOf("\r\n\t");

    public String format(PerformanceLogType type, ProcessingFeedback pf) {
        StringBuilder sb = new StringBuilder()
                .append(type.name())
                .append(SEPERATOR)
                .append(pf.getFilterName())
                .append(SEPERATOR)
                .append(pf.getFilterInstance())
                .append(SEPERATOR)
                .append(pf.getScore())
                .append(SEPERATOR)
                .append(Optional.fromNullable(pf.getResultState()).or(FilterResultState.OK))
                .append(SEPERATOR)
                .append(filterUiHint(pf));



        return sb.toString();
    }

    private String filterUiHint(ProcessingFeedback pf) {
        return LINEBREAK_REPLACER.removeFrom(pf.getUiHint());
    }
}
