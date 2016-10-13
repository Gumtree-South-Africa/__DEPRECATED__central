package com.ecg.messagebox.persistence;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.persistence.jsonconverter.JsonConverter;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.replyts.core.runtime.persistence.JacksonAwareObjectMapperConfigurer;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.jayway.awaitility.Duration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static java.util.Optional.empty;
import static org.junit.Assert.assertEquals;

public class DefaultResponseDataRepositoryIntegrationTest {

    private static final String UID1 = "u1";
    private static final String CID = "c1";

    private static DefaultResponseDataRepository responseDataRepository;

    private static CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();
    private static String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();
    private static Session session;

    @BeforeClass
    public static void init() {
        try {
            session = casdb.loadSchema(keyspace, "cassandra_new_messagebox_schema.cql", "cassandra_schema.cql");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        responseDataRepository = new DefaultResponseDataRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE, 30);
    }

    @After
    public void cleanup() {
        casdb.cleanTables(session, keyspace);
    }


    @Test
    public void shouldAddOrUpdateResponseData() {
        DateTime creationDate = DateTime.now();
        ResponseData expectedResponseData = new ResponseData(UID1, CID, creationDate, com.ecg.messagecenter.persistence.MessageType.ASQ, 50);

        responseDataRepository.addOrUpdateResponseDataAsync(expectedResponseData);

        await()
                .pollInterval(fibonacci())
                .atMost(Duration.TWO_SECONDS)
                .until(() -> responseDataRepository.getResponseData(UID1).size() == 1);

        List<ResponseData> responseDataList = responseDataRepository.getResponseData(UID1);

        assertEquals(expectedResponseData, responseDataList.get(0));
    }
}
