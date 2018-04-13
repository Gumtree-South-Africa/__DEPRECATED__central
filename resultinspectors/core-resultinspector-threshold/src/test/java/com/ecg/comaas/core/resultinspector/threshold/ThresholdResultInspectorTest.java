package com.ecg.comaas.core.resultinspector.threshold;

import com.ecg.comaas.core.resultinspector.threshold.ThresholdResultInspector;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ThresholdResultInspectorTest {

    @Mock
    private ProcessingFeedback score50NoState;

    @Mock
    private ProcessingFeedback score100NoState;

    @Mock
    private ProcessingFeedback score0StateHeld;

    @Mock
    private ProcessingFeedback score0StateDropped;

    @Mock
    private ProcessingFeedback score0StateAcceptAndTerminate;

    @Mock
    private ProcessingFeedback score100NoStateButEvaluation;

    private final List<ProcessingFeedback> feedback = new ArrayList<ProcessingFeedback>();

    private final ThresholdResultInspector ri = new ThresholdResultInspector("instance", 100, 200);

    @Before
    public void setUp() throws Exception {
        mock(score50NoState, 50, FilterResultState.OK);
        mock(score100NoState, 100, FilterResultState.OK);
        mock(score0StateHeld, 0, FilterResultState.HELD);
        mock(score0StateDropped, 0, FilterResultState.DROPPED);
        mock(score0StateAcceptAndTerminate, 0, FilterResultState.ACCEPT_AND_TERMINATE);
        mock(score100NoStateButEvaluation, 100, FilterResultState.OK);
        when(score100NoStateButEvaluation.isEvaluation()).thenReturn(true);

    }

    private void mock(ProcessingFeedback p, int score, FilterResultState state) {
        when(p.getScore()).thenReturn(score);
        when(p.getResultState()).thenReturn(state);
    }

    @Test
    public void doesNotAppendResultWhenNoFeedbackThere() throws Exception {
        ri.inspect(feedback);
        assertTrue(feedback.isEmpty());
    }



    @Test
    public void doesNotAppendScoreWhenTotalScoreBelowThreshold() throws Exception {
        feedback.add(score50NoState);
        ri.inspect(feedback);
        assertEquals(1, feedback.size());
    }

    @Test
    public void addsHeldFeedbackWhenThresholdReached() throws Exception {
        feedback.add(score100NoState);
        ri.inspect(feedback);
        assertEquals(2, feedback.size());
        assertEquals(FilterResultState.HELD, feedback.get(1).getResultState());
    }

    @Test
    public void addsDroppedFeedbackWhenThresholdReached() throws Exception {
        feedback.add(score100NoState);
        feedback.add(score100NoState);
        ri.inspect(feedback);
        assertEquals(3, feedback.size());
        assertEquals(FilterResultState.DROPPED, feedback.get(2).getResultState());
    }

    @Test
    public void skipsEvaluationOnAcceptAndTerminate() throws Exception {

        feedback.add(score100NoState);
        feedback.add(score0StateAcceptAndTerminate);
        ri.inspect(feedback);
        assertEquals(2, feedback.size());
    }

    @Test
    public void doesNotAddHeldWhenAlreadyDropped() throws Exception {
        feedback.add(score100NoState);
        feedback.add(score0StateDropped);
        ri.inspect(feedback);
        assertEquals(2, feedback.size());
    }

    @Test
    public void doesNotAddHeldWhenAlreadyHeld() throws Exception {
        feedback.add(score100NoState);
        feedback.add(score0StateHeld);
        ri.inspect(feedback);
        assertEquals(2, feedback.size());

    }

    @Test
    public void doesNotAddDroppedWhenAlreadyDropped() throws Exception {
        feedback.add(score100NoState);
        feedback.add(score100NoState);
        feedback.add(score0StateDropped);
        ri.inspect(feedback);
        assertEquals(3, feedback.size());

    }

    @Test
    public void ignoresEvaluationFeedback() throws Exception {
        feedback.add(score100NoStateButEvaluation);
        ri.inspect(feedback);
        assertEquals(1, feedback.size());

    }
}
