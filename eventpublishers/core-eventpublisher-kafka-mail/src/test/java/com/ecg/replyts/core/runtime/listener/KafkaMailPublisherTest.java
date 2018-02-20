package com.ecg.replyts.core.runtime.listener;

import com.ecg.replyts.core.runtime.persistence.mail.StoredMail;
import com.google.common.io.ByteStreams;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.ecg.replyts.core.runtime.persistence.mail.StoredMail.extract;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMailPublisherTest {

    private MailPublisher mailPublisher;

    @Mock
    private KafkaProducer<String, byte[]> producer;
    @Captor
    private ArgumentCaptor<ProducerRecord<String, byte[]>> producedMessagesCaptor;

    @Before
    public void setUp() throws Exception {
        mailPublisher = new KafkaMailPublisher(producer, "test.core.mail");
    }

    @Test
    public void testPublishEvents() throws Exception {
        byte[] data1 = new byte[]{0x01, 0x00, 0x00};
        byte[] data2 = new byte[]{0x02, 0x00, 0x00};

        mailPublisher.publishMail("key1", data1, Optional.of(data2));
        verify(producer).send(producedMessagesCaptor.capture());
        ProducerRecord<String, byte[]> m = producedMessagesCaptor.getValue();
        assertThat(m.topic(), is("test.core.mail"));
        assertThat(m.key(), is("key1"));
        assertEqualZipEntries(uncompress(m.value()), uncompress(new StoredMail(data1, Optional.of(data2)).compress()));
        StoredMail sm = extract(m.value());
        assertThat(sm.getInboundContents(), is(data1));
        assertThat(sm.getOutboundContents().orElse(null), is(data2));
    }

    private static void assertEqualZipEntries(Map<String, byte[]> actual, Map<String, byte[]> expected) {
        for (Map.Entry<String, byte[]> e : actual.entrySet()) {
            String actualEntry = e.getKey();
            byte[] payload = e.getValue();
            assertTrue(expected.containsKey(actualEntry));
            assertTrue(Arrays.equals(payload, expected.get(actualEntry)));
        }
    }

    private static Map<String, byte[]> uncompress(byte[] zippedStuff) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zippedStuff))) {
            Map<String, byte[]> entries = new HashMap<>();
            for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
                entries.put(e.getName(), ByteStreams.toByteArray(zis));
            }
            return entries;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}