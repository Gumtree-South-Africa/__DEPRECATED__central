package com.ecg.de.mobile.replyts.demand;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public class DemandReportingPostProcessor implements PostProcessor {

    private final int order;
    private final DemandReportingHandlerFactory demandReportingHandlerFactory;
    private BehaviorTrackingHandler behaviorTrackingHandler;

    public DemandReportingPostProcessor(DemandReportingHandlerFactory demandReportingHandlerFactory,
                                        BehaviorTrackingHandler behaviorTrackingHandler,
                                        int order) {
        this.demandReportingHandlerFactory = demandReportingHandlerFactory;
        this.behaviorTrackingHandler = behaviorTrackingHandler;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        demandReportingHandlerFactory.createDemandReportingHandler().handle(messageProcessingContext);
        behaviorTrackingHandler.handle(messageProcessingContext);
    }
}
