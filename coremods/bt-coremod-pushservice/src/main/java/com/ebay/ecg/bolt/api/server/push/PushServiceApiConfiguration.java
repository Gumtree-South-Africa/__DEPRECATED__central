package com.ebay.ecg.bolt.api.server.push;

import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import com.ecg.replyts.core.webapi.SpringContextProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.ecg.bolt.domain")
@ComponentScan("com.ebay.ecg.bolt.platform")
public class PushServiceApiConfiguration {
    private static final String MONGO_DATABASE_NAME = "PushService";

    @Value("${pushservice.mongo.url}")
    private String mongoUri;

    @Value("${pushservice.mongo.maxTimeout}")
    private int maxWaitTime;

    @Value("${pushservice.mongo.connectionsPerHost}")
    private int connectionsPerHost;

    @Value("${pushservice.mongo.connectTimeout}")
    private int connectionTimeout;

    @Value("${pushservice.mongo.socketTimeout}")
    private int socketTimeout;

    @Bean
    public MongoTemplate pushServiceDatabase() throws Exception {
        MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoUri, new MongoClientOptions.Builder()
          .maxWaitTime(maxWaitTime)
          .connectionsPerHost(connectionsPerHost)
          .connectTimeout(connectionTimeout)
          .socketTimeout(socketTimeout)
          .readPreference(ReadPreference.secondaryPreferred())));

        return new MongoTemplate(mongoClient, MONGO_DATABASE_NAME);
    }

    @Bean
    public SpringContextProvider pushServiceContext(ApplicationContext context) {
        return new SpringContextProvider("/v1.0.0/ps", PushServiceWebConfiguration.class, context);
    }
}