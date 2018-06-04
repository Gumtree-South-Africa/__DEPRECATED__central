package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static com.ecg.replyts.core.runtime.indexer.TestUtil.defaultMessage;
import static com.ecg.replyts.core.runtime.indexer.TestUtil.makeConversation;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class Conversation2KafkaTest {

    @Mock
    private Document2KafkaSink document2KafkaSink;

    @Mock
    private KafkaSinkService kafkaSinkService;

    private Conversation2Kafka conversation2Kafka;
    private Conversation conversation;
    private Message message0;
    private Message message1;

    @Before
    public void setup() {

        conversation2Kafka = new Conversation2Kafka();

        ReflectionTestUtils.setField(conversation2Kafka, "document2KafkaSink", document2KafkaSink);
        ReflectionTestUtils.setField(document2KafkaSink, "documentSink", kafkaSinkService);

        ImmutableConversation.Builder cBuilder = makeConversation();
        message0 = defaultMessage("msgid0").build();
        message1 = defaultMessage("msgid1").build();
        cBuilder.withMessages(Arrays.asList(message0, message1));
        conversation = cBuilder.build();
    }

    @Test
    public void updateWithConversation() {
        conversation2Kafka.updateSearchSync(Collections.singletonList(conversation));
        verify(document2KafkaSink).pushToKafka(Arrays.asList(conversation));
    }

}