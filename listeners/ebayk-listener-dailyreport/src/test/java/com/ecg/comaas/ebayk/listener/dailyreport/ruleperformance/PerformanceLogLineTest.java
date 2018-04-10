package com.ecg.comaas.ebayk.listener.dailyreport.ruleperformance;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author mhuttar
 */
@RunWith(MockitoJUnitRunner.class)
public class PerformanceLogLineTest {

    @Mock
    private ProcessingFeedback pf;

    private PerformanceLogLine line = new PerformanceLogLine();

    @Before
    public void setUp() throws Exception {
        when(pf.getFilterName()).thenReturn("com.acme.CoyoteFilter");
        when(pf.getFilterInstance()).thenReturn("roadrunner-catcher");
        when(pf.getScore()).thenReturn(50);
        when(pf.getUiHint()).thenReturn("bunch-of-dynamite");
        when(pf.getResultState()).thenReturn(FilterResultState.OK);

    }

    @Test
    public void encodesDataNormally() throws Exception {

        assertEquals("FIRE\tcom.acme.CoyoteFilter\troadrunner-catcher\t50\tOK\tbunch-of-dynamite",
                line.format(PerformanceLogType.FIRE, pf));
    }

    @Test
    public void stripsForbiddenCharsFromUiHint() throws Exception {
        when(pf.getUiHint()).thenReturn("bunch\r\nof\tdynamite");
        assertEquals("FIRE\tcom.acme.CoyoteFilter\troadrunner-catcher\t50\tOK\tbunchofdynamite",
                line.format(PerformanceLogType.FIRE, pf));

    }
}
