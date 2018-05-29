/**
 * 
 */
package com.ecg.comaas.mde.postprocessor.demandreporting.client.internal;

import com.mongodb.MongoOptions;


/**
 * As MongoOptions currently don't have setter/getter and therefore can't be configured by Spring beans directly, we
 * need this factory class.
 * 
 * @author sklaetschke
 * 
 */
public class MongoOptionsFactory {

    public MongoOptionsFactory() {}

    public static MongoOptions create(int concurrentConnections, int connectTimeout, int socketTimeout) {
        final MongoOptions options = new MongoOptions();
        options.connectionsPerHost = concurrentConnections;
        options.connectTimeout = connectTimeout;
        options.socketTimeout = socketTimeout;
        return options;
    }

}
