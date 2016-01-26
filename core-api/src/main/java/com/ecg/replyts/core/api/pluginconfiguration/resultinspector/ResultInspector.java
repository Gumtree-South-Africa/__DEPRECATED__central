package com.ecg.replyts.core.api.pluginconfiguration.resultinspector;

import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;

import java.util.List;

/**
 * Result Inspectors are invoked on each mail after it was processed by the filter chain. it looks at the aggregated
 * output of all filters and can decide on a final message state.
 *
 * @author mhuttar
 */
public interface ResultInspector {

    /**
     * inspects the aggregated filter output of a message and may extend it with further feedback (E.g. this mail needs
     * to be blocked)
     *
     * @param feedback the list so far, mutable, new feedback can be added
     */
    void inspect(List<ProcessingFeedback> feedback);
}
