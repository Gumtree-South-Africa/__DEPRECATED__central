package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.DemandReport;
import com.ecg.comaas.mde.postprocessor.demandreporting.client.ReadingDemandReportingClient;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;

import static com.ecg.comaas.mde.postprocessor.demandreporting.client.internal.MongoSupport.*;


public class MongoReadingDemandReportingClient implements ReadingDemandReportingClient, Closeable {

    final static Logger logger = LoggerFactory.getLogger(MongoReadingDemandReportingClient.class);

    final DB db;

    final ExecutorService executorService = Executors.newCachedThreadPool();

    public MongoReadingDemandReportingClient(String mongoHostUrls, MongoOptions options) {
        final List<ServerAddress> mongoHosts = parseMongoHosts(mongoHostUrls);
        if (mongoHosts.isEmpty()) throw new IllegalArgumentException("empty mongo hosts");

        final Mongo writeMongo = new Mongo(mongoHosts, options);
        db = writeMongo.getDB("demand_reporting");
        db.slaveOk();
    }

    @Override
    public void close() {
        executorService.shutdown();
        db.getMongo().close();
    }

    @Override
    public Map<Long, DemandReport> findByCustomerId(String eventType, long customerId) {
        final DBObject query = new BasicDBObjectBuilder().add(FIELD_CUSTOMER_ID, String.valueOf(customerId)).get();

        return findDemandByQuery(getCollection(db, eventType), query, MongoSupport.FIELD_OVERALL);
    }

    private Map<Long, DemandReport> findByCustomerId(String eventType, long customerId, String subReport) {
        final DBObject query = new BasicDBObjectBuilder().add(FIELD_CUSTOMER_ID, String.valueOf(customerId)).get();

        return findDemandByQuery(getCollection(db, eventType), query, subReport);
    }

    @Override
    public Map<Long, DemandReport> find(String eventType, long customerId, Collection<Long> adIds) {
        return find(eventType, customerId, adIds, MongoSupport.FIELD_OVERALL);
    }

    private Map<Long, DemandReport> find(String eventType, long customerId, Collection<Long> adIds,
                                         String subDocumentName) {
        DBObject in = new BasicDBObjectBuilder().add("$in", adIds).get();
        DBObject query = new BasicDBObjectBuilder().add(ReportedEventField.AD_ID, in)
                .add(FIELD_CUSTOMER_ID, String.valueOf(customerId)).get();

        return findDemandByQuery(getCollection(db, eventType), query, subDocumentName);
    }

    private Map<Long, DemandReport> find(String eventType, Collection<Long> adIds, String subDocumentName) {
        DBObject in = new BasicDBObjectBuilder().add("$in", adIds).get();
        DBObject query = new BasicDBObjectBuilder().add(ReportedEventField.AD_ID, in).get();

        return findDemandByQuery(getCollection(db, eventType), query, subDocumentName);
    }

    private Map<String, DemandReport> findByAdId(String eventType, Long adId, Collection<String> subReports) {
        DBObject query = new BasicDBObjectBuilder().add(ReportedEventField.AD_ID, adId).get();

        return findDemandByQuery(getCollection(db, eventType), query, subReports);
    }

    @Override
    public Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, final long customerId) {
        Map<String, Future<Map<Long, DemandReport>>> futures = new HashMap<String, Future<Map<Long, DemandReport>>>();

        for (final String eventType : eventTypes) {
            Callable<Map<Long, DemandReport>> callable = new Callable<Map<Long, DemandReport>>() {

                @Override
                public Map<Long, DemandReport> call() throws Exception {
                    return findByCustomerId(eventType, customerId);
                }
            };

            Future<Map<Long, DemandReport>> future = executorService.submit(callable);
            futures.put(eventType, future);
        }

        return extractReportsFromFutures(futures);
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, final long customerId,
                                                     final Collection<Long> adIds) {
        return find(eventTypes, customerId, adIds, MongoSupport.FIELD_OVERALL);
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, final long customerId,
                                                     final Collection<Long> adIds, final String day) {
        Map<String, Future<Map<Long, DemandReport>>> futures = new HashMap<String, Future<Map<Long, DemandReport>>>();

        for (final String eventType : eventTypes) {
            Callable<Map<Long, DemandReport>> callable = new Callable<Map<Long, DemandReport>>() {

                @Override
                public Map<Long, DemandReport> call() throws Exception {
                    return find(eventType, customerId, adIds, day);
                }
            };

            Future<Map<Long, DemandReport>> future = executorService.submit(callable);
            futures.put(eventType, future);
        }

        return extractReportsFromFutures(futures);
    }

    private <T> Map<String, Map<T, DemandReport>> extractReportsFromFutures(
            Map<String, Future<Map<T, DemandReport>>> futures) {
        try {
            Map<String, Map<T, DemandReport>> result = new HashMap<String, Map<T, DemandReport>>();
            for (Entry<String, Future<Map<T, DemandReport>>> entry : futures.entrySet()) {
                String eventType = entry.getKey();
                Future<Map<T, DemandReport>> future = entry.getValue();
                Map<T, DemandReport> reports = future.get();
                result.put(eventType, reports);
            }

            return result;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        MongoOptions options = new MongoOptions();
        options.connectTimeout = 100;
        options.socketTimeout = 3000;
        MongoReadingDemandReportingClient client = new MongoReadingDemandReportingClient("localhost", options);
        try {
            System.out.println(client.findByCustomerId("vip_view", 119049L));
        } finally {
            client.close();
        }
    }

    @Override
    public Map<String, Map<Long, DemandReport>> find(Collection<String> eventTypes, final Collection<Long> adIds) {
        Map<String, Future<Map<Long, DemandReport>>> futures = new HashMap<String, Future<Map<Long, DemandReport>>>();

        for (final String eventType : eventTypes) {
            Callable<Map<Long, DemandReport>> callable = new Callable<Map<Long, DemandReport>>() {

                @Override
                public Map<Long, DemandReport> call() throws Exception {
                    return find(eventType, adIds, MongoSupport.FIELD_OVERALL);
                }
            };

            Future<Map<Long, DemandReport>> future = executorService.submit(callable);
            futures.put(eventType, future);
        }

        return extractReportsFromFutures(futures);
    }

    @Override
    public Map<String, Map<Long, DemandReport>> findByCustomerId(Collection<String> eventTypes, final long customerId,
                                                                 final String subReport) {
        Map<String, Future<Map<Long, DemandReport>>> futures = new HashMap<String, Future<Map<Long, DemandReport>>>();

        for (final String eventType : eventTypes) {
            Callable<Map<Long, DemandReport>> callable = new Callable<Map<Long, DemandReport>>() {

                @Override
                public Map<Long, DemandReport> call() throws Exception {
                    return findByCustomerId(eventType, customerId, subReport);
                }
            };

            Future<Map<Long, DemandReport>> future = executorService.submit(callable);
            futures.put(eventType, future);
        }

        return extractReportsFromFutures(futures);
    }

    @Override
    public DemandReport findCustomerEvent(String eventType, long customerId, String subReport) {
        DBObject query = new BasicDBObjectBuilder().add("_id", new Long(customerId)).get();

        return findCustomerDemandByQuery(getCollection(db, eventType), query, subReport);
    }

    @Override
    public Map<String, Map<String, DemandReport>> findByAdId(Collection<String> eventTypes, final Long adId,
                                                             final Collection<String> subReport) {
        Map<String, Future<Map<String, DemandReport>>> futures = new HashMap<String, Future<Map<String, DemandReport>>>();

        for (final String eventType : eventTypes) {
            Callable<Map<String, DemandReport>> callable = new Callable<Map<String, DemandReport>>() {

                @Override
                public Map<String, DemandReport> call() throws Exception {
                    return findByAdId(eventType, adId, subReport);
                }
            };

            Future<Map<String, DemandReport>> future = executorService.submit(callable);
            futures.put(eventType, future);
        }

        return extractReportsFromFutures(futures);
    }

}
