package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ElasticsearchHealthCommand extends AbstractHealthCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);

    private static final String[] allIndices = { "_all" };
    static final String COMMAND_NAME = "elasticsearch";

    @Value("#{'${search.es.endpoints:unknown}'.split(',')}")
    private List<String> elasticSearchHosts = Collections.emptyList();

    private final Client client;

    ElasticsearchHealthCommand(Client client) {
        this.client = client;
    }

    @Override
    public ObjectNode execute() {
        ClusterHealthResponse response;
        try {
            response = this.client.admin().cluster()
                    .health(Requests.clusterHealthRequest(allIndices))
                    .actionGet();
        } catch (Exception ex) {
            return status(Status.DOWN, ex.getMessage());
        }

        return JsonObjects.builder()
                .attr("status", getStatus(response).name())
                .attr("cluster", getCluster(response))
                .attr("hosts", elasticSearchHosts)
                .build();
    }

    private ObjectNode getCluster(ClusterHealthResponse response) {
        return JsonObjects.builder()
                .attr("clusterName", response.getClusterName())
                .attr("numberOfNodes", response.getNumberOfNodes())
                .attr("numberOfDataNodes", response.getNumberOfDataNodes())
                .attr("activePrimaryShards", response.getActivePrimaryShards())
                .attr("activeShards", response.getActiveShards())
                .attr("relocatingShards", response.getRelocatingShards())
                .attr("initializingShards", response.getInitializingShards())
                .attr("unassignedShards", response.getUnassignedShards())
                .build();
    }

    private Status getStatus(ClusterHealthResponse response) {
        switch (response.getStatus()) {
            case GREEN:
            case YELLOW:
                return Status.UP;
            case RED:
            default:
                return Status.DOWN;
        }
    }

    static String getSearchClusterVersion(Client elasticClient) throws InterruptedException {
        if (elasticClient == null) {
            return UNKNOWN;
        }

        try {
            NodesInfoResponse response = elasticClient.admin().cluster().prepareNodesInfo().execute().get();
            return StreamSupport.stream(response.spliterator(), false)
                    .map(info -> info.getVersion().toString())
                    .collect(Collectors.joining(","));
        } catch (ExecutionException | ElasticsearchException e) {
            LOG.error("Could not get ES version", e);
            return UNKNOWN;
        }
    }

    @Override
    public String name() {
        return COMMAND_NAME;
    }
}
