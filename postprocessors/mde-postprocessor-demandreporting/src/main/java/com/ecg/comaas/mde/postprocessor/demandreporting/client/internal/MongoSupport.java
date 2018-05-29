package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.ecg.comaas.mde.postprocessor.demandreporting.client.DemandReport;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;


public class MongoSupport {

    final static Logger logger = LoggerFactory.getLogger(MongoReadingDemandReportingClient.class);

    final static String FIELD_CUSTOMER_ID = "customer_id";

    final static String FIELD_OVERALL = "overall";

    static List<ServerAddress> parseMongoHosts(String mongoHostUrls) {
        final List<ServerAddress> mongoHosts = new ArrayList<ServerAddress>(1);
        if (isNullOrEmpty(mongoHostUrls)) return Collections.emptyList();

        for (String url : mongoHostUrls.split(",\\s*")) {
            if (isNullOrEmpty(url)) continue;
            try {
                ServerAddress serverAddress = new ServerAddress(url);
                mongoHosts.add(serverAddress);
            } catch (UnknownHostException e) {
                logger.error("server address invalid ", e);
            }
        }
        return mongoHosts;
    }

    static boolean isValidJsonKey(String arg) {
        // don't allow '.' to avoid substructures
        return !isNullOrEmpty(arg) && !arg.contains(".");
    }

    static boolean isNullOrEmpty(String arg) {
        return arg == null || arg.trim().length() == 0;
    }

    static Map<Long, DemandReport> findDemandByQuery(DBCollection collection, DBObject query, String period) {
        DBObject keys = BasicDBObjectBuilder.start(period, 1).add(ReportedEventField.AD_ID, 1).get();
        logger.debug("Query: {} - Keys: {}", query, keys);
        final DBCursor cursor = collection.find(query, keys);
        final Map<Long, DemandReport> reports = new HashMap<Long, DemandReport>();
        while (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            DemandReport demandReport = convert(dbObject, period);
            if (demandReport != null) {
                Number adId = (Number) dbObject.get(ReportedEventField.AD_ID);
                reports.put(adId.longValue(), demandReport);
            }
        }
        return reports;
    }

    static DemandReport findCustomerDemandByQuery(DBCollection collection, DBObject query, String period) {
        DBObject keys = BasicDBObjectBuilder.start(period, 1).get();
        logger.debug("Query: {} - Keys: {}", query, keys);
        final DBCursor cursor = collection.find(query, keys);
        if (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            return convert(dbObject, period);
        }
        return null;
    }

    static Map<String, DemandReport> findDemandByQuery(DBCollection collection, DBObject query,
                                                       Collection<String> periods) {
        logger.debug("Query: {}", query);
        final DBCursor cursor = collection.find(query);
        final Map<String, DemandReport> reports = new HashMap<String, DemandReport>();
        if (cursor.hasNext()) {
            DBObject dbObject = cursor.next();
            for (String period : periods) {
                reports.put(period, convert(dbObject, period));
            }
        }
        return reports;
    }

    static DemandReport convert(DBObject dbObject, String period) {
        DBObject subDocument = (DBObject) dbObject.get(period);
        if (subDocument == null) {
            // logger.debug("Missing field '{}' in mongo object", subDocumentName);
            return null;
        }
        DemandReport.Builder builder = new DemandReport.Builder();

        DBObject publishers = (DBObject) subDocument.get(ReportedEventField.PUBLISHER);
        if (publishers != null) {
            builder.perPublisher(convert2Map(publishers));
        }
        DBObject referrers = (DBObject) subDocument.get(ReportedEventField.REFERRER);
        if (referrers != null) {
            builder.perReferrer(convert2Map(referrers));
        }

        Number total = (Number) subDocument.get("total");
        builder.total(total.longValue());
        return builder.build();
    }

    static Map<String, Long> convert2Map(DBObject dbObject) {
        Map<String, Long> map = new HashMap<String, Long>();
        for (String key : dbObject.keySet()) {
            Object o = dbObject.get(key);
            if (o instanceof Number) {
                Number amount = (Number) dbObject.get(key);
                map.put(key, amount.longValue());
            } else {
                logger.warn("Document contains unexpected sub documents. {}", dbObject);
            }
        }
        return map;
    }

    static DBCollection getCollection(DB db, String eventType) {
        return db.getCollection(eventType + "_events");
    }

    static String getFormattedMonth() {
        return new SimpleDateFormat("yyyy-MM").format(new Date());
    }

    static String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    static MongoOptions defaultMongoOptions(final int concurrentConnections) {
        final MongoOptions options = new MongoOptions();
        options.connectionsPerHost = concurrentConnections;
        options.connectTimeout = 100;
        options.socketTimeout = 300;
        return options;
    }

}
