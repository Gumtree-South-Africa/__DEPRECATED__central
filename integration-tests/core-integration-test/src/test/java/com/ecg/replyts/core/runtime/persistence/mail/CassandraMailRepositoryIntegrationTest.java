package com.ecg.replyts.core.runtime.persistence.mail;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Session;
import com.ecg.replyts.core.api.model.mail.MailCreationDate;
import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class CassandraMailRepositoryIntegrationTest {
    private static final String MESSAGE_ID = "123qwe";
    private static final byte[] PAYLOAD = "payload".getBytes();

    private String KEYSPACE = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName("m_");
    private DefaultCassandraMailRepository repository;
    private Session session;
    private CassandraIntegrationTestProvisioner casdb = CassandraIntegrationTestProvisioner.getInstance();

    @Before
    public void init() {
        session = casdb.initStdSchema(KEYSPACE);
        repository = new DefaultCassandraMailRepository(session, ConsistencyLevel.ONE, ConsistencyLevel.ONE);
    }

    @After
    public void cleanupTables() {
        casdb.cleanTables(session, KEYSPACE);
    }

    @Test
    public void testStoringMailStoresPayload() throws IOException {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        Optional<byte[]> response = repository.doLoad(MESSAGE_ID);
        Assert.assertTrue(response.isPresent());
        Assert.assertArrayEquals(PAYLOAD, response.get());
    }

    @Test
    public void testStoringMailShouldStoreIndex() throws IOException {
        repository.doPersist(MESSAGE_ID, PAYLOAD);

        DateTime creationDate = new DateTime();
        assertEquals(creationDate.toLocalDate(), repository.getMailCreationDate(MESSAGE_ID).toLocalDate());

        List<MailCreationDate> creationDates = repository.streamMailCreationDatesByDay(creationDate.getYear(), creationDate.getMonthOfYear(),
                creationDate.getDayOfMonth()).collect(Collectors.toList());
        assertEquals(1, creationDates.size());
        assertEquals(MESSAGE_ID, creationDates.get(0).getMailId());
        assertEquals(creationDate.toLocalDate(), creationDates.get(0).getCreationDateTime().toLocalDate());
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

        DateTime creationDate = new DateTime();

        Stream<String> shouldBeEmpty = repository.getMailIdsOlderThan(creationDate.plusMinutes(1), 100);
        assertTrue(shouldBeEmpty.count() == 0);

        assertNull(repository.getMailCreationDate(MESSAGE_ID));

        assertEquals(0, repository.streamMailCreationDatesByDay(creationDate.getYear(), creationDate.getMonthOfYear(), creationDate.getDayOfMonth()).count());

        assertNull(repository.getMailCreationDate(MESSAGE_ID));
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
