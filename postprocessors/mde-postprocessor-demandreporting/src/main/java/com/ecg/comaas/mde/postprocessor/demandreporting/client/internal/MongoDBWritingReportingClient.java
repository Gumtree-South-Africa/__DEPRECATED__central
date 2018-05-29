package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.Event;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.StorageTooSlowException;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.WritingDemandReportingClient;
import com.mongodb.*;

import java.io.Closeable;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.ecg.comaas.mde.postprocessor.demandreporting.client.internal.MongoSupport.*;


public class MongoDBWritingReportingClient implements WritingDemandReportingClient, Closeable {

    private final ExecutorService executorService;

    private final DB db;

    private final int maxPoolSize;

    private final boolean active;

    private final ScheduledExecutorService dateUpdater;

    private volatile String currentMonth = getFormattedMonth();

    private volatile String today = getFormattedDate();

    public MongoDBWritingReportingClient(String mongoHostUrls, int concurrentConnections) throws UnknownHostException,
            MongoException {
        this(mongoHostUrls, defaultMongoOptions(concurrentConnections));
    }

    public MongoDBWritingReportingClient(String mongoHostUrls, int concurrentConnections, String dbName)
            throws UnknownHostException, MongoException {
        this(mongoHostUrls, defaultMongoOptions(concurrentConnections), dbName);
    }

    public MongoDBWritingReportingClient(String mongoHostUrls, MongoOptions options) throws UnknownHostException,
            MongoException {
        this(mongoHostUrls, options, "demand_reporting");
    }

    public MongoDBWritingReportingClient(String mongoHostUrls, MongoOptions options, String dbName)
            throws UnknownHostException, MongoException {
        final List<ServerAddress> mongoHosts = parseMongoHosts(mongoHostUrls);
        active = !mongoHosts.isEmpty();

        if (active) {
            maxPoolSize = options.connectionsPerHost * 100;

            final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(maxPoolSize);
            executorService = new ThreadPoolExecutor(1, options.connectionsPerHost, 5, TimeUnit.MINUTES, queue);

            final Mongo writeMongo = new Mongo(mongoHosts, options);
            db = writeMongo.getDB(dbName);
            db.slaveOk();

            dateUpdater = Executors.newScheduledThreadPool(1);
            dateUpdater.scheduleAtFixedRate(new DateUpdaterTask(this), 0l, 10l, TimeUnit.MINUTES);
        } else {
            db = null;
            executorService = null;
            dateUpdater = null;
            maxPoolSize = 0;
        }

    }

    @Override
    public void report(Event event) throws StorageTooSlowException {
        report(event.getAdId(), event.getCustomerId(), event.getPublisher(), event.getReferrer(), event.getEventType());
    }

    @Override
    @Deprecated
    public void report(long adId, long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException {
        if (!active) return;

        final Map<String, String> eventProperties = new HashMap<String, String>();
        if (isValidJsonKey(publisher)) {
            eventProperties.put(ReportedEventField.PUBLISHER, publisher);
        }
        if (isValidJsonKey(referrer)) {
            eventProperties.put(ReportedEventField.REFERRER, referrer);
        }

        final Map<String, Map<String, String>> combinedEventProperties = new HashMap<String, Map<String, String>>();
        if (isValidJsonKey(referrer)) {
            Map<String, String> featureDetails = new HashMap<String, String>();
            featureDetails.put(referrer, isValidJsonKey(publisher) ? publisher : null);
            combinedEventProperties.put(ReportedEventField.FEATURES, featureDetails);
        }

        try {
            MongoDBEntryUpdater updater = new MongoDBEntryUpdater(adId, Collections.singletonMap(FIELD_CUSTOMER_ID,
                    Long.toString(customerId)), eventProperties, combinedEventProperties, currentMonth, today,
                    getCollection(db, eventType));
            executorService.execute(updater);
        } catch (RejectedExecutionException e) {
            throw new StorageTooSlowException("More than " + maxPoolSize + " store request in queue.");
        }
    }

    /**
     * increase counter by one which is the normal way to add an event
     */
    @Override
    public void reportCustomerEvent(long customerId, String publisher, String referrer, String eventType)
            throws StorageTooSlowException {
        if (!active) return;

        final Map<String, String> eventProperties = new HashMap<String, String>();
        if (isValidJsonKey(publisher)) {
            eventProperties.put(ReportedEventField.PUBLISHER, publisher);
        }
        if (isValidJsonKey(referrer)) {
            eventProperties.put(ReportedEventField.REFERRER, referrer);
        }

        try {
            MongoDBCustomerEntryUpdater updater = new MongoDBCustomerEntryUpdater(customerId, eventProperties,
                    currentMonth, today, getCollection(db, eventType));
            executorService.execute(updater);
        } catch (RejectedExecutionException e) {
            throw new StorageTooSlowException("More than " + maxPoolSize + " store request in queue.");
        }
    }

    /**
     * increase counter by more than one event which is a special case used by migration jobs for bulk updates
     */
    @Override
    public void reportCustomerEvents(long customerId, String publisher, String referrer, String eventType,
                                     int numVisits, String date) throws StorageTooSlowException {
        if (!active) return;

        final Map<String, String> eventProperties = new HashMap<String, String>();
        if (isValidJsonKey(publisher)) {
            eventProperties.put(ReportedEventField.PUBLISHER, publisher);
        }
        if (isValidJsonKey(referrer)) {
            eventProperties.put(ReportedEventField.REFERRER, referrer);
        }

        try {
            MongoDBCustomerEntryBulkUpdater updater = new MongoDBCustomerEntryBulkUpdater(customerId, eventProperties,
                    currentMonth, date, getCollection(db, eventType), numVisits);
            executorService.execute(updater);
        } catch (RejectedExecutionException e) {
            throw new StorageTooSlowException("More than " + maxPoolSize + " store request in queue.");
        }
    }

    void assertActive() {
        if (!active) throw new IllegalStateException("not active");
    }

    public void shutdown() {
        if (active) {
            db.getMongo().close();
            executorService.shutdown();
            dateUpdater.shutdown();
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    private static class DateUpdaterTask implements Runnable {

        private final MongoDBWritingReportingClient client;

        DateUpdaterTask(MongoDBWritingReportingClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            client.today = getFormattedDate();
            client.currentMonth = getFormattedMonth();
        }

    }

}
