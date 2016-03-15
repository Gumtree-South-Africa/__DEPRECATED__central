package com.ecg.replyts.core.runtime.persistence.mail;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.cassandra.EmbeddedCassandra;
import org.joda.time.DateTime;
import org.junit.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class CassandraMailRepositoryIntegrationTest {

    private static final String MESSAGE_ID = "123qwe";
    private static final byte[] PAYLOAD = "payload".getBytes();
    private static final String KEYSPACE = "replyts2_mail_test";
    private static CassandraMailRepository repository;
    private static Session session;
    private static final EmbeddedCassandra casdb = EmbeddedCassandra.getInstance();

    @BeforeClass
    public static void init() {
        session = casdb.initStdSchema(KEYSPACE);
        repository = new CassandraMailRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session,KEYSPACE);
    }

    @Test
    public void testStoringMail() throws IOException {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        Optional<byte[]> response = repository.doLoad(MESSAGE_ID);
        Assert.assertTrue(response.isPresent());
        Assert.assertArrayEquals(PAYLOAD, response.get());
    }

    @Test
    public void testDeletingMailRemovesTheMail() {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        repository.deleteMail(MESSAGE_ID);

        Optional<byte[]> payload = repository.doLoad(MESSAGE_ID);
        Assert.assertFalse(payload.isPresent());
    }

    @Test
    public void testDeleteMailRemovesMailIndex() {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        repository.deleteMail(MESSAGE_ID);

        Stream<String> shouldBeEmpty = repository.getMailIdsOlderThan(new DateTime().plusMinutes(1), 100);
        Assert.assertTrue(shouldBeEmpty.count() == 0);

        List<Row> rowsInIndexTable = session.execute("SELECT * from core_mail_creation_idx where mail_id = ?", MESSAGE_ID).all();
        Assert.assertTrue(rowsInIndexTable.isEmpty());
    }

    @Test
    public void testGetMailIdsOlderThanWhenParameterEqualsEmailCreationDateThenReturnsMailId() {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        List<String> mailIds = repository.getMailIdsOlderThan(new DateTime().plusMinutes(1), 100).collect(Collectors.toList());
        Assert.assertTrue(mailIds.contains(MESSAGE_ID));
    }


    @Test
    public void testGetMailIdsOlderThanWhenParameterBeforeEmailCreationDateThenDoesNotReturnMailId() {
        DateTime now = DateTime.parse("2015-10-01");
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        List<String> mailIds = repository.getMailIdsOlderThan(now.minusMinutes(1), 100).collect(Collectors.toList());
        Assert.assertFalse(mailIds.contains(MESSAGE_ID));
    }

}
