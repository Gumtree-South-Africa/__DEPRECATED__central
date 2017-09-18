package com.ecg.replyts.core.webapi.control.health;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CassandraHealthCommand extends AbstractHealthCommand {

    static final Select CASSANDRA_LOCAL_VERSION =
            QueryBuilder.select("release_version")
                    .from("system", "local");

    static final Select CASSANDRA_CLUSTER_DETAILS =
            QueryBuilder.select("peer", "data_center", "host_id", "rack", "release_version", "schema_version")
                    .from("system", "peers");

    static final String COMMAND_NAME = "cassandra";

    @Value("#{'${persistence.cassandra.core.endpoint:unknown}'.split(',')}")
    private List<String> cassandraHosts = Collections.emptyList();

    private final Map<String, Session> cassandraSessions;
    private final Map<String, ConsistencyLevel> consistencyLevels;


    CassandraHealthCommand(Map<String, Session> cassandraSessions, Map<String, ConsistencyLevel> consistencyLevels) {
        this.cassandraSessions = cassandraSessions;
        this.consistencyLevels = consistencyLevels;
    }

    @Override
    public ObjectNode execute() {
        ArrayNode connections = cassandraSessions.entrySet().stream()
                .map(CassandraHealthCommand::checkConnection)
                .collect(ARRAY_NODE_COLLECTOR);

        ArrayNode consistencies = consistencyLevels.entrySet().stream()
                .map(CassandraHealthCommand::mapConsistency)
                .collect(ARRAY_NODE_COLLECTOR);

        return JsonObjects.builder()
                .attr("status", getOverallStatus(connections).name())
                .attr("connections", connections)
                .attr("consistencyLevels", consistencies)
                .attr("cluster", getCluster())
                .attr("hosts", cassandraHosts)
                .build();
    }

    @Override
    public String name() {
        return COMMAND_NAME;
    }

    private static ObjectNode checkConnection(Map.Entry<String, Session> entry) {
        try {
            entry.getValue().execute(CASSANDRA_LOCAL_VERSION);
            return status(entry.getKey(), Status.UP);
        } catch (Exception ex) {
            return status(entry.getKey(), Status.DOWN, ex.getMessage());
        }
    }

    private static ObjectNode mapConsistency(Map.Entry<String, ConsistencyLevel> entry) {
        return nameValue(entry.getKey(), entry.getValue().name());
    }

    private JsonNode getCluster() {
        Session session = cassandraSessions.get("cassandraSessionForCore");
        if (session == null) {
            return error("Cassandra Session called [cassandraSessionForCore] is not available.");
        }

        ResultSet result = session.execute(CASSANDRA_CLUSTER_DETAILS);
        Map<String, Map<String, List<Peer>>> peers = StreamSupport.stream(result.spliterator(), false)
                .map(Peer::mapper)
                .collect(Collectors.groupingBy(peer -> peer.dataCenter, Collectors.groupingBy(peer -> peer.rack)));

        return peers.entrySet().stream()
                .map(CassandraHealthCommand::mapDatacenter)
                .collect(ARRAY_NODE_COLLECTOR);
    }

    private static ObjectNode mapDatacenter(Map.Entry<String, Map<String, List<Peer>>> entry) {
        ArrayNode racks = entry.getValue().entrySet().stream()
                .map(CassandraHealthCommand::mapRack)
                .collect(ARRAY_NODE_COLLECTOR);

        return JsonObjects.builder()
                .attr("name", entry.getKey())
                .attr("racks", racks)
                .build();
    }

    private static ObjectNode mapRack(Map.Entry<String, List<Peer>> entry) {
        ArrayNode peers = entry.getValue().stream()
                .map(Peer::mapToNode)
                .collect(ARRAY_NODE_COLLECTOR);

        return JsonObjects.builder()
                .attr("name", entry.getKey())
                .attr("peers", peers)
                .build();
    }

    private static class Peer {

        final String peer;
        final String dataCenter;
        final String hostId;
        final String rack;
        final String version;
        final String schema;

        private Peer(String peer, String dataCenter, String hostId, String rack, String version, String schema) {
            this.peer = peer;
            this.dataCenter = dataCenter;
            this.hostId = hostId;
            this.rack = rack;
            this.version = version;
            this.schema = schema;
        }

        private static Peer mapper(Row row) {
            return new Peer(row.getString("peer"), row.getString("data_center"), row.getString("host_id"),
                    row.getString("rack"), row.getString("release_version"), row.getString("schema_version"));
        }

        private static ObjectNode mapToNode(Peer peer) {
            return JsonObjects.builder()
                    .attr("peer", peer.peer)
                    .attr("hostId", peer.hostId)
                    .attr("version", peer.version)
                    .attr("scheme", peer.schema)
                    .build();
        }
    }
}
