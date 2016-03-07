package com.ecg.de.mobile.replyts.demand;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Test;

import static org.mockito.Mockito.*;


public class DemandReportingPostProcessorTest {

    @Test
    public void callsBehaviorTrackingAndDemandReportingOnPostprocess() throws Exception {
        MessageProcessingContext context = mock(MessageProcessingContext.class);
        BehaviorTrackingHandler behaviorTrackingHandler = mock(BehaviorTrackingHandler.class);
        DemandReportingHandlerFactory demandReportingHandlerFactory = mock(DemandReportingHandlerFactory.class);
        DemandReportingHandler demandReportingHandler = mock(DemandReportingHandler.class);
        when(demandReportingHandlerFactory.createDemandReportingHandler()).thenReturn(demandReportingHandler);
        DemandReportingPostProcessor postProcessor = new DemandReportingPostProcessor(demandReportingHandlerFactory, behaviorTrackingHandler, 1);
        postProcessor.postProcess(context);


        verify(demandReportingHandler).handle(context);
        verify(behaviorTrackingHandler).handle(context);
    }
}
