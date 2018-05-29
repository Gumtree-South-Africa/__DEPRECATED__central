package com.ecg.comaas.mde.postprocessor.demandreporting.client.rest.internal;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.Event;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.StorageTooSlowException;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.WritingDemandReportingClient;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.rest.DemandReportingHttpClient;

import java.util.concurrent.*;


/**
 * NOT READY FOR PRODUCTION YET
 */
@Deprecated
public class RestApiWritingDemandReportingClient implements WritingDemandReportingClient {

    private final DemandReportingHttpClient httpClient;

    private final ExecutorService executorService;

    private final String baseUrl;

    public RestApiWritingDemandReportingClient(DemandReportingHttpClient httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        int maxConcurrentConnections = httpClient.getMaxConcurrentConnections();
        int maxPoolSize = maxConcurrentConnections * 100;

        final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(maxPoolSize);
        executorService = new ThreadPoolExecutor(1, maxConcurrentConnections, 5, TimeUnit.MINUTES, queue);
    }

    @Override
    public void report(long adId, long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void report(Event event) throws StorageTooSlowException {
        try {
            executorService.execute(new RestApiWriter(event, baseUrl, httpClient));
        } catch (RejectedExecutionException e) {
            throw new StorageTooSlowException(e);
        }
    }

    @Override
    public void reportCustomerEvent(long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportCustomerEvents(long customerId, String publisher, String referrer, String eventType,
                                     int numVisits, String date) throws StorageTooSlowException {
        throw new IllegalAccessError("Method not implemented!");
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
