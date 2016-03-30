package com.ecg.replyts.integration.elasticsearch;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class EnvironmentBasedNodeConfigurator {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentBasedNodeConfigurator.class);

    private final String clusterName;
    private final String endpoints;

    static final String INDEX_NAME = "replyts";

    public EnvironmentBasedNodeConfigurator(String clusterName, String endpoints) {
        this.clusterName = clusterName;
        this.endpoints = endpoints;
    }

    public Node forEmbeddedEnvironments() {
        LOG.warn("Initializing ElasticSearch Embedded Standalone server. cluster {}. THIS IS AN AUTOMATION TEST ONLY MODE", clusterName, endpoints);

        Builder settingsBuilder = basicSettings(clusterName)
                .put("http.enabled", false)
                .put("node.local", true)
                .put("node.client", false)
                .put("node.data", true)
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("discovery.zen.ping.multicast", false)
                .put("path.data", "target/es-data-" + UUID.randomUUID());

        Node node = createRunningEsNode(settingsBuilder);

        LOG.info("Deleting index " + INDEX_NAME);

        IndicesAdminClient indicesAdmn = node.client().admin().indices();

        deleteIndexIfAvailable(indicesAdmn);

        LOG.info("Creating index " + INDEX_NAME);

        createIndex(indicesAdmn);

        return node;
    }

    private Node createRunningEsNode(Builder settingsBuilder) {
        return nodeBuilder()
                .settings(settingsBuilder)
                .build()
                .start();
    }

    private void createIndex(IndicesAdminClient indicesAdminClient) {
        try {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME)
                    .mapping("message", getMessageMappingConfiguration());
            indicesAdminClient
                    .create(createIndexRequest)
                    .actionGet(10, TimeUnit.SECONDS);

            UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest(INDEX_NAME)
                    .settings(settingsBuilder()
                        .put("index.translog.flush_threshold_ops", 1)
                        .put("index.translog.interval", "1s")
                        .put("index.translog.flush_threshold_period", "1s")
                        .build());
            indicesAdminClient
                    .updateSettings(updateSettingsRequest)
                    .actionGet(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOG.error("could not create new ReplyTS index ", e);
            throw new RuntimeException("could not create elasticsearch index!", e);
        }
    }

    private Builder basicSettings(String clusterName) {
        return settingsBuilder()
                .put("cluster.name", clusterName);
    }

    private void deleteIndexIfAvailable(IndicesAdminClient indicesAdminClient) {
        try {
            DeleteIndexResponse deleteIndexResponse = indicesAdminClient
                    .prepareDelete(INDEX_NAME)
                    .execute()
                    .actionGet(10, TimeUnit.SECONDS);

            if (!deleteIndexResponse.isAcknowledged())
                LOG.info("Elasticsearch did not delete the index " + INDEX_NAME);

        } catch (Exception e) {
            LOG.info("could not delete index: " + e.getMessage());
        }
    }

    private String getMessageMappingConfiguration() {
        try (InputStreamReader is = new InputStreamReader(getClass().getResourceAsStream("/example/elasticsearch/message_mapping.json"), Charsets.UTF_8)) {
            return CharStreams.toString(is);
        } catch (IOException e) {
            throw new RuntimeException("could not read message_mapping.json file - can't create search index", e);
        }
    }

}
