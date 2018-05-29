package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.Event;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.WritingDemandReportingClient;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

class DemandReportingHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DemandReportingHandler.class);
    private static final String EVENT_TYPE = "contact_email";

    private final WritingDemandReportingClient writingDemandReportingClient;

    DemandReportingHandler(WritingDemandReportingClient writingDemandReportingClient) {
        this.writingDemandReportingClient = writingDemandReportingClient;
    }

    public void handle(MessageProcessingContext messageProcessingContext) {
        try {
            if (reportDemand(messageProcessingContext)) {
                String adIdString = messageProcessingContext.getConversation()
                        .getAdId();
                long adId = Utils.adIdStringToAdId(adIdString);
                long customerId = customerId(messageProcessingContext);
                String publisher = publisher(messageProcessingContext);
                Event.Builder eventBuilder = new Event.Builder().adId(adId)
                        .customerId(customerId).publisher(publisher).eventType(EVENT_TYPE);
                writingDemandReportingClient.report(eventBuilder.get());
            }
        } catch (Exception e) {
            LOG.error("Error while reporting demand.", e);
        }
    }

    private static Long customerId(MessageProcessingContext messageProcessingContext) {
        String customerId = messageProcessingContext.getConversation().getCustomValues().get("customer_id");
        if (!StringUtils.hasText(customerId)) {
            customerId = messageProcessingContext.getMessage().getHeaders().get("X-Cust-Customer_Id");
        }
        return Utils.toLong(customerId);
    }

    private static String publisher(MessageProcessingContext messageProcessingContext) {
        String publisher = messageProcessingContext.getConversation().getCustomValues().get("publisher");
        if (!StringUtils.hasText(publisher)) {
            publisher = messageProcessingContext.getMessage().getHeaders().get("X-Cust-Publisher");
        }
        return publisher;
    }

    private static boolean reportDemand(MessageProcessingContext messageProcessingContext) {
        String demand = messageProcessingContext.getMessage().getHeaders().get("X-Report-Demand");
        return StringUtils.hasText(demand) && "true".equals(demand);
    }
}
