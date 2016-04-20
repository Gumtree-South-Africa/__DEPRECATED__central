package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.cassandra.CassandraIntegrationTestProvisioner;
import com.ecg.replyts.integration.test.IntegrationTestRunner;
import com.google.common.io.ByteStreams;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.*;
import org.subethamail.wiser.WiserMessage;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * All-is-well integration tests.
 */
public class SunnyDayAcceptanceTest {
    private static final long deliveryTimeoutMillis = 20_000;

    private CassandraIntegrationTestProvisioner CASDB = CassandraIntegrationTestProvisioner.getInstance();

    private String keyspace = CassandraIntegrationTestProvisioner.createUniqueKeyspaceName();

    private IntegrationTestRunner runner = new IntegrationTestRunner(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("persistence.cassandra.keyspace", keyspace);

        return properties;
    }).get(), "/integrationtest-conf");

    @Before
    public void startReplytsAndClearMessages() throws Exception {
        CASDB.initStdSchema(keyspace);

        runner.start();
        runner.clearMessages();
    }

    @After
    public void shutdown() {
        runner.stop();
    }

    @Test
    public void rtsProcessedAnAsqMailAndAReply() throws Exception {
        deliverMailToRts("plain-asq.eml");
        WiserMessage anonymizedAsq = runner.waitForMessageArrival(1, deliveryTimeoutMillis);
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
        WiserMessage anonymizedAsqReply = runner.waitForMessageArrival(2, deliveryTimeoutMillis);
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
        runner.getMailSender().sendMail(emlData);
    }

    private void deliverReplyMailToRts(String emlName) throws Exception {
        WiserMessage asqMessage = runner.getLastRtsSentMail();
        String anonymousAsqSender = asqMessage.getEnvelopeSender();

        byte[] bytes = ByteStreams.toByteArray(getClass().getResourceAsStream(emlName));
        String replyData = new String(bytes, "US-ASCII");
        replyData = replyData.replace("{{{{anonymous reply to}}}}", anonymousAsqSender);
        runner.getMailSender().sendMail(replyData.getBytes("US-ASCII"));
    }
}
