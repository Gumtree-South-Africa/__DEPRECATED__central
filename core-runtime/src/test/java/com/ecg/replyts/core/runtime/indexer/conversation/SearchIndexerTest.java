package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;


@RunWith(SpringRunner.class)
public class SearchIndexerTest {

    protected Client client;
    protected MailCloakingService mailCloakingService;
    protected IndexRequestBuilder indexBuilder;

    protected SearchIndexer searchIndexer;

    @Before
    public void setup() throws InterruptedException, ExecutionException, TimeoutException {
        MockitoAnnotations.initMocks(this);

        client = mock(Client.class, RETURNS_DEEP_STUBS);
        mailCloakingService = mock(MailCloakingService.class);
        BulkRequestBuilder brb = mock(BulkRequestBuilder.class, RETURNS_DEEP_STUBS);
        BulkResponse resp = mock(BulkResponse.class);
        ListenableActionFuture<BulkResponse> laf = mock(ListenableActionFuture.class);
        when(brb.execute()).thenReturn(laf);
        when(laf.get(anyLong(), any(TimeUnit.class))).thenReturn(resp);

        indexBuilder = mock(IndexRequestBuilder.class);
        when(client.prepareIndex(anyString(), anyString(), anyString())).thenReturn(indexBuilder);
        when(client.prepareBulk()).thenReturn(brb);
        when(mailCloakingService.createdCloakedMailAddress(anyObject(), anyObject())).thenReturn(new MailAddress("anonymized@test.com"));
        when(indexBuilder.setSource(any(XContentBuilder.class))).thenReturn(indexBuilder);
        when(indexBuilder.setTTL(anyLong())).thenReturn(indexBuilder);

        searchIndexer = new SearchIndexer(client, new IndexDataBuilder(mailCloakingService));
    }

    @Test
    public void updateWithTwoMessagesWritesTwoUpdates() {
        Conversation conversation = makeConversation(2);

        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        verify(client, times(1)).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid0"));
        verify(client, times(1)).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid1"));
    }

    @Test
    public void updateWritesCorrectJsonContent() throws Exception {
        Conversation conversation = makeConversation(1);

        ArgumentCaptor<XContentBuilder> captor = ArgumentCaptor.forClass(XContentBuilder.class);

        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("anonymous-buyer@test.com"));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress("anonymous-seller@test.com"));

        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        verify(client, times(1)).prepareIndex("replyts", "message", "id/msgid0");
        verify(indexBuilder, times(1)).setSource(captor.capture());

        XContentBuilder contentBuilder = captor.getValue();
        String output = contentBuilder.string();

        // Note that emails are indexed lowercased because API searches explicitly do the same
        String expectedOutput = "{\"toEmail\":\"buyer@test.com\",\"toEmailAnonymous\":\"anonymous-buyer@test.com\",\"fromEmail\":\"seller@test.com\",\"fromEmailAnonymous\":\"anonymous-seller@test.com\",\"messageDirection\":\"SELLER_TO_BUYER\",\"messageState\":\"SENT\",\"humanResultState\":\"UNCHECKED\",\"receivedDate\":\"2012-01-30T19:01:52.000Z\",\"conversationStartDate\":\"2012-01-29T18:01:52.000Z\",\"messageText\":\"some text\",\"adId\":\"myAd#123\",\"attachments\":[],\"lastEditor\":null,\"lastModified\":\"2012-01-30T19:01:52.000Z\",\"customHeaders\":{},\"feedback\":[]}";
        assertEquals(expectedOutput, output);
    }


    @MockBean
    private Document2KafkaSink document2KafkaSink;

    @Test
    public void updatesESOnly() {
        Conversation conversation = SearchIndexerTest.makeConversation(2);

        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        verify(client).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid0"));
        verify(client).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid1"));

        verify(document2KafkaSink, never()).pushToKafka(anyListOf(Conversation.class));
    }


    @Test
    public void updatesESAndKafka() {
        searchIndexer.enableIndexing2Kafka = false;
        searchIndexer.document2KafkaSink = document2KafkaSink;

        Conversation conversation = SearchIndexerTest.makeConversation(2);

        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        verify(client).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid0"));
        verify(client).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid1"));

        verify(document2KafkaSink).pushToKafka(anyListOf(Conversation.class));
    }

    @Test
    public void updatesKafkaOnly() {
        searchIndexer.enableIndexing2Kafka = true;
        searchIndexer.document2KafkaSink = document2KafkaSink;

        Conversation conversation = SearchIndexerTest.makeConversation(2);

        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        verify(client, never()).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid0"));

        verify(document2KafkaSink).pushToKafka(anyListOf(Conversation.class));
    }

    private static Conversation makeConversation(int messageCount) {
        Builder builder = aConversation().withId("id")
                .withCreatedAt(new DateTime(2012, 1, 29, 19, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 30, 19, 1, 55, DateTimeZone.forID("Europe/Amsterdam")))
                .withBuyer("Buyer@test.com", "buy3R")
                .withSeller("seller@Test.com", "s3ll3R")
                .withState(ConversationState.ACTIVE)
                .withAdId("myAd#123");

        for (int i = 0; i < messageCount; i++) {
            builder.withMessage(defaultMessage("msgid" + i));
        }

        return builder.build();
    }

    private static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder defaultMessage(String id) {
        return aMessage()
                .withId(id)
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withHeader("Subject", "Hello subject")
                .withTextParts(Collections.singletonList("some text"));
    }
}