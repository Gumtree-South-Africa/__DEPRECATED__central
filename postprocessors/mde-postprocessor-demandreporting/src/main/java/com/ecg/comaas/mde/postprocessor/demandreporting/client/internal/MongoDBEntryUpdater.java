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


public class MongoDBEntryUpdater implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBEntryUpdater.class);

    private final DBCollection dbCollection;

    private final String currentMonth;

    private final String today;

    private final long adId;

    private final Map<String, String> adProperties;

    private final Map<String, String> eventProperies;

    private final Map<String, Map<String, String>> combinedEventProperties;

    public MongoDBEntryUpdater(long adId, Map<String, String> adProperties, Map<String, String> eventProperties,
                               Map<String, Map<String, String>> combinedEventProperties, String currentMonth, String today,
                               DBCollection dbCollection) {
        this.adId = adId;
        this.adProperties = adProperties;
        this.eventProperies = eventProperties;
        this.combinedEventProperties = combinedEventProperties;
        this.currentMonth = currentMonth;
        this.today = today;
        this.dbCollection = dbCollection;
    }

    @Override
    public void run() {
        try {
            BasicDBObject query = new BasicDBObject();
            query.put(ReportedEventField.AD_ID, adId);
            for (Entry<String, String> entry : adProperties.entrySet()) {
                query.put(entry.getKey(), entry.getValue());
            }

            dbCollection.update(query, createUpdateObject(), true, false);
        } catch (RuntimeException e) {
            LOGGER.error("could not update report", e);
        }
    }

    private DBObject createUpdateObject() {
        BasicDBObjectBuilder incrementUpdateObjectBuilder = new BasicDBObjectBuilder();

        for (Entry<String, String> property : eventProperies.entrySet()) {

            addKeyToIncrementObject(incrementUpdateObjectBuilder, property.getKey() + "." + property.getValue());
        }

        for (Entry<String, Map<String, String>> combinedProperty : combinedEventProperties.entrySet()) {
            for (Entry<String, String> propertyDetails : combinedProperty.getValue().entrySet()) {

                if (propertyDetails.getValue() != null) {
                    addKeyToIncrementObject(incrementUpdateObjectBuilder, combinedProperty.getKey() + "."
                            + propertyDetails.getKey() + "." + propertyDetails.getValue());
                }

                addKeyToIncrementObject(incrementUpdateObjectBuilder,
                        combinedProperty.getKey() + "." + propertyDetails.getKey() + ".total");
            }
        }

        addKeyToIncrementObject(incrementUpdateObjectBuilder, "total");

        DBObject result = new BasicDBObjectBuilder().add("$inc", incrementUpdateObjectBuilder.get()).get();
        return result;
    }

    private BasicDBObjectBuilder addKeyToIncrementObject(BasicDBObjectBuilder updateObjectBuilder, String key) {
        updateObjectBuilder.add(FIELD_OVERALL + "." + key, 1);
        updateObjectBuilder.add(currentMonth + "." + key, 1);
        updateObjectBuilder.add(today + "." + key, 1);
        return updateObjectBuilder;
    }

}
