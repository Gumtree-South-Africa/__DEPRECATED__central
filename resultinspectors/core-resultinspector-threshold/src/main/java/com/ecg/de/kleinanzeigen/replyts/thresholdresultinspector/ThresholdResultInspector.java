package com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;

import java.util.List;

/**
 * Result inspector that will sum up all processing feedback scores and:
 * <ul>
 *     <li>If any filter reports= ACCEPT_AND_TERMINATE skip</li>
 *     <li>If sum >= heldThreshold AND mail is not yet BLOCKED, put it to HELD</li>
 *     <li>If sum >= blockedThreshold, put it to DROPPED</li>
 * </ul>*/
public class ThresholdResultInspector implements ResultInspector {
    private final String instanceName;
    private final long heldThreshold;
    private final long blockedThreshold;

    public ThresholdResultInspector(String instanceName, long heldThreshold, long blockedThreshold) {
        this.instanceName = instanceName;
        this.heldThreshold = heldThreshold;
        this.blockedThreshold = blockedThreshold;
    }

    public void inspect(List<ProcessingFeedback> processingFeedbacks) {
        long sum = 0;
        FilterResultState worstState = FilterResultState.OK;
        for (ProcessingFeedback feedback : processingFeedbacks) {
            // don't count evaluation feedback, as this is only for testing new filters and should not be counted on results.
            if (feedback.isEvaluation()) {
                continue;
            }

            Integer score = feedback.getScore();
            if (score != null) {
                sum += score;
            }

            FilterResultState feedbacksResultState = feedback.getResultState();
            if (feedbacksResultState == FilterResultState.ACCEPT_AND_TERMINATE) {
                // ACCEPT_AND_TERMINATE is the wildcard for sending that message out.
                return;
            }

            // Only allow transitions in the direction from OK to DROPPED, not the other way.
            if (worstState.hasLowerPriorityThan(feedbacksResultState)) {
                worstState = feedbacksResultState;
            }
        }

        FilterResultState resultInspectorsRecommendation = matchScoreAgainstThresholds(sum);

        boolean recommendationIsWorstState = worstState.hasLowerPriorityThan(resultInspectorsRecommendation);
        boolean recommendationDiffersFromOutcome = resultInspectorsRecommendation != worstState;

        if (recommendationDiffersFromOutcome && recommendationIsWorstState) {
            ImmutableProcessingFeedback feedback = new ImmutableProcessingFeedback(
                    getClass().getName(),
                    instanceName,
                    resultInspectorsRecommendation.name(),
                    "Score exceeded "+resultInspectorsRecommendation+" threshold",
                    0,
                    resultInspectorsRecommendation,
                    false
            );

            processingFeedbacks.add(feedback);

        }

    }

    private FilterResultState matchScoreAgainstThresholds(long sum) {
        return sum >= blockedThreshold ?
                FilterResultState.DROPPED :
                sum >= heldThreshold ? FilterResultState.HELD :
                        FilterResultState.OK;
    }
}
