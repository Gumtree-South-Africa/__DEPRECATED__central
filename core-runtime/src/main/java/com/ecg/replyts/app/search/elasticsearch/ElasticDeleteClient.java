package com.ecg.replyts.app.search.elasticsearch;

import org.apache.http.client.utils.URIBuilder;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REMOVE THIS CLIENT ONCE ELASTIC SUPPORTS DELETE BY QUERY IN HIGH LEVEL REST CLIENT!
 */
public class ElasticDeleteClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticDeleteClient.class);

    private final JerseyClient client;
    private final JerseyWebTarget target;

    public ElasticDeleteClient(URI elasticsearchUri, String indexName, String user, String password) {
        this.client = JerseyClientBuilder.createClient();
        URI uri;
        try {
            uri = new URIBuilder()
                    .setHost(elasticsearchUri.getHost())
                    .setPort(elasticsearchUri.getPort())
                    .setPath(indexName + "/_delete_by_query")
                    .setUserInfo(user, password).build();

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
