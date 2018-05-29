package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MongoDBEntryWriter implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBEntryWriter.class);
	private final BasicDBObject object;
	private final DBCollection dbCollection;
	
	public MongoDBEntryWriter(BasicDBObject object, DBCollection dbCollection) {
		this.object = object;
		this.dbCollection = dbCollection;
	}

	@Override
	public void run() {
		try {
			dbCollection.insert(object);
			
			LOGGER.debug("Inserted object {} to collection {}.", object, dbCollection.getName());
		} catch (Exception e) {
			LOGGER.error("Could not store event.", e);
		}
	}

}
