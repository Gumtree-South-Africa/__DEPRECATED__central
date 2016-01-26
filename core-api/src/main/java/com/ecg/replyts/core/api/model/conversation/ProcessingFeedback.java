package com.ecg.replyts.core.api.model.conversation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Output from a filter or pre-processor.
 * <p/>
 * In general, filters should only report problems, as this is more helpful for message screening.
 */
@JsonTypeInfo(defaultImpl = ImmutableProcessingFeedback.class, use = JsonTypeInfo.Id.CLASS)
public interface ProcessingFeedback {
    /**
     * @return class name of the filter that generated this feedback
     */
    String getFilterName();

    /**
     * @return instance name of the filter that generated this feedback. (There could be more instances from the same
     * filter with different configurations)
     */
    String getFilterInstance();

    /**
     * @return a rule/an information bit that could be used in screening tools to highlight the reason this message was
     * filtered
     */
    String getUiHint();

    /**
     * @return a human readable version of this processing feedback for CS staff
     */
    String getDescription();

    /**
     * @return the score that was assigned by the filter for the reason, this processing feedback is about, or
     * <code>null</code> if the filter assigned a {@link #getResultState}
     */
    Integer getScore();

    /**
     * @return the message state that filter had updated this message to, or <code>null</code> if the filter did not
     * affect a message's state
     */
    FilterResultState getResultState();

    /**
     * @return true if this feedback is from a filter that is in evaluation mode, false otherwise
     */
    boolean isEvaluation();

}
