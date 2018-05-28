package com.ecg.replyts.app.search.elasticsearch;

import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REMOVE THIS CLIENT ONCE ELASTIC SUPPORTS DELETE BY QUERY IN HIGH LEVEL REST CLIENT!
 */
public class ElasticDeleteClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticDeleteClient.class);

    private final JerseyClient client;
    private final JerseyWebTarget target;

    public ElasticDeleteClient(String elasticsearchUri, String indexName) {
        this.client = JerseyClientBuilder.createClient();

        URI uri;
        try {
            uri = new URI(elasticsearchUri + "/" + indexName + "/_delete_by_query");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Incorrect parsing of Elasticsearch URI", e);
        }
        this.target = client.target(uri);
    }

    public void delete(String query) {
        Response response = target.request().post(Entity.entity(query, MediaType.APPLICATION_JSON_TYPE));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            LOG.warn("Elastisearch delete by qyery failed. StatusCode: [" + response.getStatus() + "] [" + query + "]");
        }
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
