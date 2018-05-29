package com.ecg.comaas.mde.postprocessor.demandreporting.client;

public interface WritingDemandReportingClient {

    /**
     * Report an event.
     * 
     * @param adId
     * @param customerId
     * @param publisher
     * @param referrer
     * @param eventType
     * @throws StorageTooSlowException
     * 
     *             use {@link #report(Event)}
     */
    @Deprecated
    void report(long adId, long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException;

    /**
     * Report an event.
     * 
     * @param eventType
     * @throws StorageTooSlowException
     */
    void report(Event eventType) throws StorageTooSlowException;

    /**
     * Report a customer-event (ad-unspecific).
     * 
     * @param customerId
     * @param publisher
     * @param referrer
     * @param eventType
     * @throws StorageTooSlowException
     */
    void reportCustomerEvent(long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException;

    /**
     * For bulk updates only: Report several customer-events at once
     * 
     * @param customerId
     * @param publisher
     * @param referrer
     * @param eventType
     * @param numVisits
     * @param change date
     * @throws StorageTooSlowException
     */
    void reportCustomerEvents(long customerId, String publisher, String referrer, String eventType, int numVisits,
                              String date) throws StorageTooSlowException;

}
