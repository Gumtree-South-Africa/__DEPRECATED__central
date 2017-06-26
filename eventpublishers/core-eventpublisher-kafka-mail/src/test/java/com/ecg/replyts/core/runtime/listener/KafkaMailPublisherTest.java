package com.ecg.replyts.core.runtime.listener;

import com.ecg.replyts.core.runtime.persistence.mail.StoredMail;
import com.google.common.io.ByteStreams;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.ecg.replyts.core.runtime.persistence.mail.StoredMail.extract;
import static com.google.common.base.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMailPublisherTest {

    private MailPublisher mailPublisher;

    @Mock
    private Producer<String, byte[]> producer;
    @Captor
    private ArgumentCaptor<KeyedMessage<String, byte[]>> producedMessagesCaptor;

    @Before
    public void setUp() throws Exception {
        mailPublisher = new KafkaMailPublisher(producer, "test.core.mail");
    }

    @Test
    public void testPublishEvents() throws Exception {
        byte[] data1 = new byte[]{0x01, 0x00, 0x00};
        byte[] data2 = new byte[]{0x02, 0x00, 0x00};

        mailPublisher.publishMail("key1", data1, of(data2));
        verify(producer).send(producedMessagesCaptor.capture());
        KeyedMessage<String, byte[]> m = producedMessagesCaptor.getValue();
        assertThat(m.topic(), is("test.core.mail"));
        assertThat(m.partitionKey(), is("key1"));
        assertEqualZipEntries(uncompress(m.message()), uncompress(new StoredMail(data1, of(data2)).compress()));
        StoredMail sm = extract(m.message());
        assertThat(sm.getInboundContents(), is(data1));
        assertThat(sm.getOutboundContents().orNull(), is(data2));
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