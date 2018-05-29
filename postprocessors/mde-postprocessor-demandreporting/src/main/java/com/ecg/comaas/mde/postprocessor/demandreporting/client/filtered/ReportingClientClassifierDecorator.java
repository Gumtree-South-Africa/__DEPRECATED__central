package com.ecg.comaas.mde.postprocessor.demandreporting.client.filtered;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.DemandReport;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.ReadingDemandReportingClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


// moved here from dealer-admin de.mobile.dealer.statistics.ReportingClientClassifierDecorator

/**
 * This wrapper filters out all mesa-publishers that are undefined according to apiuser-classifier. The total-value will
 * be set to the sum of the remaining publisher-entries. That means that all demand that was tracked without any
 * publisher is also filtered. The referrer information is not changed.
 */
public class ReportingClientClassifierDecorator implements ReadingDemandReportingClient {

    private final ReadingDemandReportingClient demandReportingClient;

    private final DemandKeyReviewer demandKeyReviewer;

    public ReportingClientClassifierDecorator(ReadingDemandReportingClient demandReportingClient,
                                              DemandKeyReviewer demandKeyReviewer) {
        this.demandReportingClient = demandReportingClient;
        this.demandKeyReviewer = demandKeyReviewer;
    }

    @Override
    public Map<Long, DemandReport> findByCustomerId(String eventType, long customerId) {
        return filterDemandReportMap(demandReportingClient.findByCustomerId(eventType, customerId));
    }

    @Override
    public Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, long customerId) {
        return filterFullDemandReport(demandReportingClient.findByCustomerId(eventTypes, customerId));
    }

    @Override
    public Map<Long, DemandReport> find(String eventType, long customerId, Collection<Long> adIds) {
        return filterDemandReportMap(demandReportingClient.find(eventType, customerId, adIds));
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, long customerId,
                                                     Collection<Long> adIds) {
        return filterFullDemandReport(demandReportingClient.find(eventTypes, customerId, adIds));
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, long customerId,
                                                     Collection<Long> adIds, String day) {
        return filterFullDemandReport(demandReportingClient.find(eventTypes, customerId, adIds, day));
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, Collection<Long> adIds) {
        return filterFullDemandReport(demandReportingClient.find(eventTypes, adIds));
    }

    @Override
    public Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, long customerId,
                                                                 String timeField) {
        return filterFullDemandReport(demandReportingClient.findByCustomerId(eventTypes, customerId, timeField));
    }

    @Override
    public DemandReport findCustomerEvent(String eventType, long customerId, String timeField) {
        return demandReportingClient.findCustomerEvent(eventType, customerId, timeField);
    }

    @Override
    public Map<String, Map<String, DemandReport>> findByAdId(Collection<String> eventTypes, Long adId,
                                                             Collection<String> timeFields) {
        return filterFullDemandReport(demandReportingClient.findByAdId(eventTypes, adId, timeFields));
    }

    private <T> Map<String, Map<T, DemandReport>> filterFullDemandReport(Map<String, Map<T, DemandReport>> fullReport) {

        if (fullReport == null) return null;

        Map<String, Map<T, DemandReport>> filteredFullReport = new HashMap<String, Map<T, DemandReport>>();

        for (Map.Entry<String, Map<T, DemandReport>> entry : fullReport.entrySet()) {
            filteredFullReport.put(entry.getKey(), filterDemandReportMap(entry.getValue()));
        }

        return filteredFullReport;
    }

    private <T> Map<T, DemandReport> filterDemandReportMap(Map<T, DemandReport> demandReportMap) {

        if (demandReportMap == null) return null;

        Map<T, DemandReport> filteredDemandReportMap = new HashMap<T, DemandReport>();

        for (Map.Entry<T, DemandReport> entry : demandReportMap.entrySet()) {
            filteredDemandReportMap.put(entry.getKey(), filterDemandReport(entry.getValue()));
        }

        return filteredDemandReportMap;
    }

    /**
     * @param demandReport
     * @return
     */
    private DemandReport filterDemandReport(DemandReport demandReport) {

        if (demandReport == null) return null;

        Map<String, Long> perReferrer = demandReport.getPerReferrer();
        Map<String, Long> perPublisher = demandReport.getPerPublisher();

        Map<String, Long> perPublisherFiltered = new HashMap<String, Long>();
        long total = 0;

        for (Map.Entry<String, Long> entry : perPublisher.entrySet()) {
            if (demandKeyReviewer.includeDemandFrom(entry.getKey())) {
                perPublisherFiltered.put(entry.getKey(), entry.getValue());
                total += entry.getValue();
            }
        }

        return new DemandReport.Builder().perPublisher(perPublisherFiltered).perReferrer(perReferrer).total(total)
                .build();

    }

}
