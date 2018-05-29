package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;

import static com.ecg.comaas.mde.postprocessor.demandreporting.client.internal.MongoSupport.FIELD_OVERALL;


public class MongoDBCustomerEntryBulkUpdater implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBCustomerEntryBulkUpdater.class);

    private final DBCollection dbCollection;

    private final String currentMonth;

    private final String today;

    private final long customerId;

    private final Map<String, String> eventProperies;

    private final int numEvents;

    public MongoDBCustomerEntryBulkUpdater(long customerId, Map<String, String> eventProperties, String currentMonth,
                                           String today, DBCollection dbCollection, int numEvents) {
        this.customerId = customerId;
        this.eventProperies = eventProperties;
        this.currentMonth = currentMonth;
        this.today = today;
        this.dbCollection = dbCollection;
        this.numEvents = numEvents;
    }

    @Override
    public void run() {
        try {
            BasicDBObject query = new BasicDBObject();
            query.put("_id", customerId);

            dbCollection.update(query, createUpdateObject(), true, false);
        } catch (RuntimeException e) {
            LOGGER.error("could not update report", e);
        }
    }

    private DBObject createUpdateObject() {
        BasicDBObjectBuilder incrementUpdateObjectBuilder = new BasicDBObjectBuilder();
        for (Entry<String, String> property : eventProperies.entrySet()) {
            String key = property.getKey() + "." + property.getValue();
            incrementUpdateObjectBuilder.add(FIELD_OVERALL + "." + key, numEvents);
            incrementUpdateObjectBuilder.add(currentMonth + "." + key, numEvents);
            incrementUpdateObjectBuilder.add(today + "." + key, numEvents);
        }
        incrementUpdateObjectBuilder.add(FIELD_OVERALL + ".total", numEvents);
        incrementUpdateObjectBuilder.add(currentMonth + ".total", numEvents);
        incrementUpdateObjectBuilder.add(today + ".total", numEvents);

        DBObject result = new BasicDBObjectBuilder().add("$inc", incrementUpdateObjectBuilder.get()).get();
        return result;
    }

}
