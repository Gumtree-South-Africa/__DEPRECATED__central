package com.ecg.replyts.core.runtime.listener;

import com.ecg.replyts.core.runtime.persistence.mail.StoredMail;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.ecg.replyts.core.runtime.persistence.mail.StoredMail.extract;
import static com.google.common.base.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
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
        assertThat(m.message(), is(new StoredMail(data1, of(data2)).compress()));
        StoredMail sm = extract(m.message());
        assertThat(sm.getInboundContents(), is(data1));
        assertThat(sm.getOutboundContents().orNull(), is(data2));
    }
}