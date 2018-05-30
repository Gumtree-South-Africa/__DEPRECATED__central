package com.ecg.comaas.mde.postprocessor.demandreporting.client;

public interface WritingDemandReportingClient {

    void report(Event eventType) throws StorageTooSlowException;

}
