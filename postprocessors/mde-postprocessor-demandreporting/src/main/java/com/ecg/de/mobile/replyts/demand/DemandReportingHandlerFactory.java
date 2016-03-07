package com.ecg.de.mobile.replyts.demand;

import de.mobile.reporting.demand.client.WritingDemandReportingClient;

public class DemandReportingHandlerFactory {
    private WritingDemandReportingClient writingDemandReportingClient;


    public DemandReportingHandlerFactory(WritingDemandReportingClient writingDemandReportingClient) {
        this.writingDemandReportingClient = writingDemandReportingClient;
    }

    public DemandReportingHandler createDemandReportingHandler() {
        return new DemandReportingHandler(writingDemandReportingClient);
    }
}
