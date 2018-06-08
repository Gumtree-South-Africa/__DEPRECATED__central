package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MailAddress;
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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Document2KafkaTest {

    private IndexDataBuilder indexDataBuilder;
    private DocumentSink documentSink;
    @Mock
    private MailCloakingService mailCloakingService;
    @Mock
    private KafkaSinkService kafkaSinkService;

    private Conversation conversation;
    private Message message0;
    private Message message1;
    private static final String TENANT = "SOME_TENANT";

    @Before
    public void setup() {

        when(mailCloakingService.createdCloakedMailAddress(anyObject(), anyObject())).thenReturn(new MailAddress("anonymized@test.com"));
        indexDataBuilder = new IndexDataBuilder(mailCloakingService);
        documentSink = new Document2KafkaSink();

        ReflectionTestUtils.setField(documentSink, "indexDataBuilder", indexDataBuilder);
        ReflectionTestUtils.setField(documentSink, "documentSink", kafkaSinkService);
        ReflectionTestUtils.setField(documentSink, "tenant", TENANT);

        ImmutableConversation.Builder cBuilder = makeConversation();
        message0 = defaultMessage("msgid0").build();
        message1 = defaultMessage("msgid1").build();
        cBuilder.withMessages(Arrays.asList(message0, message1));
        conversation = cBuilder.build();
    }

    @Test
    public void pushToKafkaConversations() {
        documentSink.sink(Collections.singletonList(conversation));
        verify(kafkaSinkService).storeAsync(eq(TENANT + "/" + conversation.getId() + "/" + message0.getId()), anyObject());
    }

    @Test
    public void pushToKafkaConversation() {
        documentSink.sink(conversation);
        verify(kafkaSinkService).storeAsync(eq(TENANT + "/" + conversation.getId() + "/" + message0.getId()), anyObject());
    }

    @Test
    public void pushToKafkaConversationAndMessage() {
        documentSink.sink(conversation, message0.getId());
        documentSink.sink(conversation, message1.getId());
        verify(kafkaSinkService).storeAsync(eq(TENANT + "/" + conversation.getId() + "/" + message0.getId()), anyObject());
        verify(kafkaSinkService).storeAsync(eq(TENANT + "/" + conversation.getId() + "/" + message1.getId()), anyObject());
    }
}