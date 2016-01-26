package com.ecg.replyts.core.api.webapi.model;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;

/**
 * The result of a filter action.
 *
 * @author huttar
 */
public interface ProcessingFeedbackRts {

    /**
     * @return the score the filter assigned to this message (optional. Most probably, there will be either a score or a
     * state)
     */
    Integer getScore();

    /**
     * @return if <code>true</code>, the filter was in evaluation mode (testing only) and that result was not taken into
     * account when deciding on the message's fate
     */

    Boolean getEvaluation();

    /**
     * @return the Status this filter had assigned to the message (optional. Most probably, there will be eithe ra score
     * or a state)
     */
    FilterResultState getState();

    /**
     * @return filter type that had produced this feedback
     */
    String getFilterName();

    /**
     * @return the actual running instance of that filter (there may be many filter instances with different
     * configurations)
     */
    String getFilterInstance();

    /**
     * @return semantic information about what was filtered that can be used by client webapps for visualisation
     */
    String getUiHint();

    /**
     * @return human readable description about what was filtered
     */
    String getDescription();

}
