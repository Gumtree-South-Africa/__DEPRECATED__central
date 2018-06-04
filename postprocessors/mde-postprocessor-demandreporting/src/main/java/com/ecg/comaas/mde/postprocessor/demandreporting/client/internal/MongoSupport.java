package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class MongoSupport {

    final static Logger logger = LoggerFactory.getLogger(MongoSupport.class);

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
