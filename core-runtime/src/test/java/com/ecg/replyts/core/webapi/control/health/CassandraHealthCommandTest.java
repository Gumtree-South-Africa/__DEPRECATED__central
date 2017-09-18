package com.ecg.replyts.core.webapi.control.health;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

public class CassandraHealthCommandTest {

    @Test
    public void testConsistencyLevelsJson() {
        HashMap<String, ConsistencyLevel> consistencyLevels = new HashMap<>();
        consistencyLevels.put("cassandraReadConsistency", ConsistencyLevel.LOCAL_QUORUM);
        consistencyLevels.put("cassandraWriteConsistency", ConsistencyLevel.LOCAL_SERIAL);

        CassandraHealthCommand command = new CassandraHealthCommand(new HashMap<>(), consistencyLevels);
        ObjectNode result = command.execute();

        Map<String, JsonNode> connections = map((ArrayNode) result.get("consistencyLevels"));
        assertEquals(2, connections.size());

        JsonNode connection1 = connections.get("cassandraReadConsistency");
        assertEquals("cassandraReadConsistency", connection1.get("name").asText());
        assertEquals(ConsistencyLevel.LOCAL_QUORUM.name(), connection1.get("value").asText());

        JsonNode connection2 = connections.get("cassandraWriteConsistency");
        assertEquals("cassandraWriteConsistency", connection2.get("name").asText());
        assertEquals(ConsistencyLevel.LOCAL_SERIAL.name(), connection2.get("value").asText());
    }

    @Test
    public void testSessionCheckJson() {
        Session session = Mockito.mock(Session.class);
        Map<String, Session> cassandraSessions = new HashMap<>();
        cassandraSessions.put("cassandraSessionForCore", session);
        cassandraSessions.put("cassandraSessionForMb", session);

        mockSessionCheck(session);
        mockClusterCall(session);

        CassandraHealthCommand command = new CassandraHealthCommand(cassandraSessions, new HashMap<>());
        ObjectNode result = command.execute();

        Map<String, JsonNode> connections = map((ArrayNode) result.get("connections"));
        assertEquals(2, connections.size());

        JsonNode connection = connections.get("cassandraSessionForCore");
        assertEquals("cassandraSessionForCore", connection.get("name").asText());
        assertEquals("UP", connection.get("status").asText());
    }

    @Test
    public void testClusterErrorSession() {
        CassandraHealthCommand command = new CassandraHealthCommand(new HashMap<>(), new HashMap<>());
        ObjectNode result = command.execute();
        assertEquals("Cassandra Session called [cassandraSessionForCore] is not available.", result.get("cluster").get("error").asText());
    }

    @Test
    public void testClusterJson() {
        Session session = Mockito.mock(Session.class);
        Map<String, Session> cassandraSessions = new HashMap<>();
        cassandraSessions.put("cassandraSessionForCore", session);

        mockClusterCall(session);
        mockSessionCheck(session);

        CassandraHealthCommand command = new CassandraHealthCommand(cassandraSessions, new HashMap<>());
        ObjectNode result = command.execute();

        Map<String, JsonNode> dcs = map((ArrayNode) result.get("cluster"));
        assertEquals(2, dcs.size());

        Map<String, JsonNode> ams = map((ArrayNode) dcs.get("ams1").get("racks"));
        Map<String, JsonNode> dus = map((ArrayNode) dcs.get("dus1").get("racks"));

        assertEquals(3, ams.size());
        assertEquals(2, dus.size());

        Map<String, JsonNode> amsRack = map((ArrayNode) ams.get("zone3").get("peers"), "peer");
        assertEquals(2, amsRack.size());

        JsonNode peer1 = amsRack.get("10.41.22.9");
        assertEquals("10.41.22.9", peer1.get("peer").asText());
        assertEquals("bcfde2ba-8fc7-44e2-b857-ec2636e299ef", peer1.get("hostId").asText());
        assertEquals("2.1.15", peer1.get("version").asText());
        assertEquals("fec7cb29-264b-310a-815a-607571900611", peer1.get("scheme").asText());

        JsonNode peer2 = amsRack.get("10.41.22.2");
        assertEquals("10.41.22.2", peer2.get("peer").asText());
        assertEquals("bc4f815c-350f-4422-9f86-8de13eeb26bb", peer2.get("hostId").asText());
        assertEquals("2.1.15", peer2.get("version").asText());
        assertEquals("fec7cb29-264b-310a-815a-607571900611", peer2.get("scheme").asText());
    }

    private static Map<String, JsonNode> map(ArrayNode nodes, String keyField) {
        return StreamSupport.stream(nodes.spliterator(), false)
                .collect(Collectors.toMap(node -> node.get(keyField).asText(), Function.identity()));
    }

    private static Map<String, JsonNode> map(ArrayNode nodes) {
        return map(nodes, "name");
    }

    private static void mockClusterCall(Session session) {
        ResultSet result = Mockito.mock(ResultSet.class);
        List<Row> rows = clusterTestData();

        Mockito.doReturn(rows.spliterator()).when(result).spliterator();
        Mockito.doReturn(result).when(session).execute(CassandraHealthCommand.CASSANDRA_CLUSTER_DETAILS);
    }

    /*
     peer          | data_center | host_id                              | rack  | release_version | schema_version
    ---------------+-------------+--------------------------------------+-------+-----------------+--------------------------------------
        10.41.22.9 |        ams1 | bcfde2ba-8fc7-44e2-b857-ec2636e299ef | zone3 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
        10.39.13.6 |        dus1 | fb22f99b-a77f-4c7e-b0d1-658303a1ff32 | zone2 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
        10.41.22.1 |        ams1 | 3a78ba07-43dc-4b48-bf4c-4e3849f1c08d | zone2 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
        10.41.22.2 |        ams1 | bc4f815c-350f-4422-9f86-8de13eeb26bb | zone3 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
        10.41.22.8 |        ams1 | 1fffda38-f266-4409-9e66-fd66bcc915f9 | zone1 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
        10.39.13.9 |        dus1 | 5c7f458a-d857-40d8-a668-addbdb13273c | zone1 |          2.1.15 | fec7cb29-264b-310a-815a-607571900611
     */
    private static List<Row> clusterTestData() {
        Row row1 = StubRow.clusterRow("10.41.22.9", "ams1", "bcfde2ba-8fc7-44e2-b857-ec2636e299ef", "zone3", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");
        Row row2 = StubRow.clusterRow("10.39.13.6", "dus1", "fb22f99b-a77f-4c7e-b0d1-658303a1ff32", "zone2", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");
        Row row3 = StubRow.clusterRow("10.41.22.1", "ams1", "3a78ba07-43dc-4b48-bf4c-4e3849f1c08d", "zone2", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");
        Row row4 = StubRow.clusterRow("10.41.22.2", "ams1", "bc4f815c-350f-4422-9f86-8de13eeb26bb", "zone3", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");
        Row row5 = StubRow.clusterRow("10.41.22.8", "ams1", "1fffda38-f266-4409-9e66-fd66bcc915f9", "zone1", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");
        Row row6 = StubRow.clusterRow("10.39.13.9", "dus1", "5c7f458a-d857-40d8-a668-addbdb13273c", "zone1", "2.1.15", "fec7cb29-264b-310a-815a-607571900611");

        List<Row> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);
        rows.add(row5);
        rows.add(row6);
        return rows;
    }

    private static void mockSessionCheck(Session session) {
        ResultSet result = Mockito.mock(ResultSet.class);
        Mockito.doReturn(result).when(session).execute(CassandraHealthCommand.CASSANDRA_LOCAL_VERSION);
    }
}
