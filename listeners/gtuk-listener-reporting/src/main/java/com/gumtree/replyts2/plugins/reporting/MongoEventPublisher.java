package com.gumtree.replyts2.plugins.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoEventPublisher implements EventPublisher {

    private static ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter COLLECTION_NAME_DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static final Logger LOG = LoggerFactory.getLogger(MongoEventPublisher.class);

    private MongoDatabase mongoDatabase;

    /**
     * Constructor.
     *
     * @param mongoClient the client to write to
     */
    public MongoEventPublisher(MongoClient mongoClient) {
        this.mongoDatabase = mongoClient.getDatabase("rts2_event_log");
    }

    @Override
    public void publish(MessageProcessedEvent event) {
        try {
            String json = mapper.writeValueAsString(event);
            MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName());
            Document document = Document.parse(json);
            collection.insertOne(document);
        } catch (Exception ex) {
            LOG.error(String.format("Failed to write event to Mongo event log for message %s in conversation %s",
                    event.getMessageId(),
                    event.getConversationId()));
        }
    }

    private String collectionName() {
        return COLLECTION_NAME_DATE_FORMAT.print(new DateTime());
    }
}
