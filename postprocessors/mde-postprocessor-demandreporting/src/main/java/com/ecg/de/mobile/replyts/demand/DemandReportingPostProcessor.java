package com.ecg.de.mobile.replyts.demand;

import com.ecg.de.mobile.replyts.demand.usertracking.UserTrackingHandler;
import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public class DemandReportingPostProcessor implements PostProcessor {

    private final int order;
    private final DemandReportingHandlerFactory demandReportingHandlerFactory;
    private BehaviorTrackingHandler behaviorTrackingHandler;
    private UserTrackingHandler userTrackingHandler;

    public DemandReportingPostProcessor(DemandReportingHandlerFactory demandReportingHandlerFactory,
                                        BehaviorTrackingHandler behaviorTrackingHandler,
                                        UserTrackingHandler userTrackingHandler,
                                        int order) {
        this.demandReportingHandlerFactory = demandReportingHandlerFactory;
        this.behaviorTrackingHandler = behaviorTrackingHandler;
        this.userTrackingHandler = userTrackingHandler;
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
        userTrackingHandler.handle(messageProcessingContext);
    }
}
