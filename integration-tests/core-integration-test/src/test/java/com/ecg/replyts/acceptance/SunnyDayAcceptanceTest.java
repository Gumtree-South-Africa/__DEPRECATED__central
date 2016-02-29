package com.ecg.replyts.acceptance;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.ecg.replyts.util.CassandraTestUtil;
import com.google.common.io.ByteStreams;
import org.cassandraunit.spring.CassandraDataSet;
import org.cassandraunit.spring.EmbeddedCassandra;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;
import org.subethamail.wiser.WiserMessage;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import org.cassandraunit.spring.CassandraUnitDependencyInjectionIntegrationTestExecutionListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.runner.RunWith;

/**
 * All-is-well integration tests.
 */
@EmbeddedCassandra(configuration = "cu-cassandra.yaml")
@CassandraDataSet(keyspace = "replyts_integration_test", value = {"cassandra_schema.cql"})
@TestExecutionListeners(CassandraUnitDependencyInjectionIntegrationTestExecutionListener.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class SunnyDayAcceptanceTest {
    private static final String KEYSPACE = "replyts_integration_test";
    private static Session session;

    @Before
    public void startReplytsAndClearMessages() throws Exception {
        Cluster cluster = Cluster.builder()
                .addContactPoints(EmbeddedCassandraServerHelper.getHost())
                .withPort(EmbeddedCassandraServerHelper.getNativeTransportPort())
                .build();
        session = cluster.connect(KEYSPACE);

        IntegrationTestRunner.setConfigResourceDirectory("/integrationtest-conf");
        IntegrationTestRunner.getReplytsRunner();
        IntegrationTestRunner.clearMessages();
    }

    @After
    public void shutdown() {
        IntegrationTestRunner.stop();
        CassandraTestUtil.cleanTables(session, "replyts_integration_test");
    }

    @Test
    public void rtsProcessedAnAsqMailAndAReply() throws Exception {
        deliverMailToRts("plain-asq.eml");
        WiserMessage anonymizedAsq = IntegrationTestRunner.waitForMessageArrival(1, 5000L);
        MimeMessage anonAsq = anonymizedAsq.getMimeMessage();
        assertThat(anonAsq.getSubject(), is("Reactie op uw advertentie: Twee matrassen, hoef je niet te draaien en wasbare hoezen"));
        assertThat(anonymizedAsq.getEnvelopeReceiver(), is("obeuga@foon.nl"));
        // asserts that sendername plugin works:
        assertHasSingleTo(anonAsq, "obeuga@foon.nl", "O. Beuga via Marktplaats");
        MatcherAssert.assertThat(anonymizedAsq.getEnvelopeSender(), com.ecg.replyts.integration.test.support.Matchers.isAnonymized());
        assertHasAnonymousFrom(anonAsq);
        assertRtsHeadersNotPresent(anonAsq);
        assertIsAnonymous(anonymizedAsq, "seller_66@hotmail.com");

        deliverReplyMailToRts("plain-asq-reply.eml");
        WiserMessage anonymizedAsqReply = IntegrationTestRunner.waitForMessageArrival(2, 5000L);
        MimeMessage anonAsqReply = anonymizedAsqReply.getMimeMessage();
        assertThat(anonAsqReply.getSubject(), is("Antw: Reactie op uw advertentie: Twee matrassen, hoef je niet te draaien en wasbare hoezen"));
        assertThat(anonymizedAsqReply.getEnvelopeReceiver(), is("seller_66@hotmail.com"));
        // asserts that sendername plugin works for replies:
        assertHasSingleTo(anonAsqReply, "seller_66@hotmail.com", "Seller66 via Marktplaats");
        MatcherAssert.assertThat(anonymizedAsqReply.getEnvelopeSender(), com.ecg.replyts.integration.test.support.Matchers.isAnonymized());
        assertHasAnonymousFrom(anonAsqReply);
        assertRtsHeadersNotPresent(anonAsqReply);
        assertIsAnonymous(anonymizedAsqReply, "obeuga@foon.nl");
    }

    private void assertHasSingleTo(MimeMessage anonAsq, String toAddress, String toPersonal) throws Exception {
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO).length, is(1));
        assertThat(anonAsq.getRecipients(Message.RecipientType.TO)[0], Matchers.<Address>is(new InternetAddress(toAddress, toPersonal)));
    }

    private void assertHasAnonymousFrom(MimeMessage anonAsq) throws MessagingException {
        assertThat(anonAsq.getFrom().length, is(1));
        MatcherAssert.assertThat(anonAsq.getFrom()[0], com.ecg.replyts.integration.test.support.Matchers.isAnonymizedAddress());
    }

    private void assertRtsHeadersNotPresent(MimeMessage anonMail) throws MessagingException {
        assertThat(anonMail.getHeader("X-ADID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-L1-CATEGORYID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-L2-CATEGORYID"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-FROM"), is(nullValue()));
        assertThat(anonMail.getHeader("X-CUST-TO"), is(nullValue()));
    }

    private void assertIsAnonymous(WiserMessage anonMail, String absentMailAddress) throws Exception {
        // NOTE: only works on emails with simple encodings, no base64 please
        assertThat(new String(anonMail.getData(), "US-ASCII"), not(containsString(absentMailAddress)));
    }

    private void deliverMailToRts(String emlName) throws Exception {
        byte[] emlData = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        IntegrationTestRunner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = IntegrationTestRunner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        byte[] bytes = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        String replyData = new String(bytes, "US-ASCII");
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        IntegrationTestRunner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }

}
