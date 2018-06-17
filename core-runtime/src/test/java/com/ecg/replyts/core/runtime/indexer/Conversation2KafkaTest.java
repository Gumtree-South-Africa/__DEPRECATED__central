package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Conversation2KafkaTest {

    @Mock
    private Document2KafkaSink documentSink;

    @Mock
    private ConversationRepository conversationRepository;

    private Conversation2Kafka conversation2Kafka;
    @Mock
    private MutableConversation conversation;

    @Before
    public void setup() {
        conversation2Kafka = new Conversation2Kafka();

        ReflectionTestUtils.setField(conversation2Kafka, "conversationRepository", conversationRepository);
        ReflectionTestUtils.setField(conversation2Kafka, "documentSink", documentSink);
        when(conversation.getId()).thenReturn("convid1");
        when(conversationRepository.getById(conversation.getId())).thenReturn(conversation);
    }

    @Test
    public void updateWithConversation() {
        conversation2Kafka.updateElasticSearch(conversation.getId());
        verify(documentSink).sink(conversation);
    }

}