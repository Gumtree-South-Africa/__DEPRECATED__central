package com.ecg.comaas.mde.postprocessor.demandreporting.client;

import java.util.Collection;
import java.util.Map;


public interface ReadingDemandReportingClient {

    /**
     * Find demand report for a customer and by eventtype.
     * 
     * @param eventType
     * @param customerId
     * @param adIds
     * @return
     */
    Map<Long, DemandReport> findByCustomerId(String eventType, long customerId);

    /**
     * Find demand report for a customer and by specified eventtypes.
     * 
     * @param eventType
     * @param customerId
     * @param adIds
     * @return
     */
    Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, long customerId);

    /**
     * Find demand report for a customer and by specified eventtypes and subReport.
     * 
     * @param eventType
     * @param customerId
     * @param adIds
     * @param subReport 'yyyy-MM-dd' or 'yyyy-MM'
     * @return
     */
    Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, long customerId,
                                                          String subReport);

    /**
     * Find demand report for a collection of ads.
     *
     * @param eventType
     * @param customerId
     * @param adIds
     * @return
     */
    Map<Long, DemandReport> find(String eventType, long customerId, Collection<Long> adIds);

    /**
     * Find demand reports for a collection of event types and ads.
     *
     * @param eventTypes
     * @param customerId
     * @param adIds
     * @return
     */
    Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, long customerId, Collection<Long> adIds);

    /**
     * Find demand reports for a collection of event types and ads, independent of customerId.
     *
     * @param eventTypes
     * @param customerId
     * @param adIds
     * @return
     */
    Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, Collection<Long> adIds);

    /**
     * Find demand reports for all sub-reports of one ad. The outer map contains event-types, the inner map all
     * subReportuments.
     *
     * @param eventTypes
     * @param customerId
     * @param adIds
     * @return
     */
    Map<String, Map<String, DemandReport>> findByAdId(Collection<String> eventTypes, Long adId,
                                                      Collection<String> subReports);

    /**
     *
     * @param eventTypes
     * @param customerId
     * @param adIds
     * @param subReports collection of 'yyyy-MM-dd', 'yyyy-MM' or 'overall'
     * @return
     */
    Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, long customerId, Collection<Long> adIds,
                                              String subReport);

    /**
     * Find demand report for a customerevent by eventtype and sub-report. Customer-events are events that are counted
     * per customer, and not per ad.
     * 
     * @param eventType
     * @param customerId
     * @param adIds
     * @param subReport 'yyyy-MM-dd', 'yyyy-MM' or 'overall'
     * @return
     */
    DemandReport findCustomerEvent(String eventType, long customerId, String subReport);

}
