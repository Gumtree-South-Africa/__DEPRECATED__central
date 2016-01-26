package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.MailAddress;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchIndexerTest {

    private Client client;
    private MailCloakingService mailCloakingService;
    private IndexRequestBuilder indexBuilder;

    private SearchIndexer searchIndexer;
    private BulkRequestBuilder brb;

    @Before
    public void setup() throws InterruptedException, ExecutionException, TimeoutException {
        MockitoAnnotations.initMocks(this);
        client = mock(Client.class, RETURNS_DEEP_STUBS);
        mailCloakingService = mock(MailCloakingService.class);
        brb = mock(BulkRequestBuilder.class, RETURNS_DEEP_STUBS);
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

        searchIndexer.updateSearchSync(Arrays.asList(conversation));

        verify(client, times(1)).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid0"));
        verify(client, times(1)).prepareIndex(eq("replyts"), eq("message"), eq("id/msgid1"));
    }

    @Test
    public void updateWritesCorrectJsonContent() throws Exception {
        Conversation conversation = makeConversation(1);

        ArgumentCaptor<XContentBuilder> captor = ArgumentCaptor.forClass(XContentBuilder.class);

        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("anonymous-buyer@test.com"));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress("anonymous-seller@test.com"));

        searchIndexer.updateSearchSync(Arrays.asList(conversation));

        verify(client, times(1)).prepareIndex("replyts", "message", "id/msgid0");
        verify(indexBuilder, times(1)).setSource(captor.capture());

        XContentBuilder contentBuilder = captor.getValue();
        String output = contentBuilder.string();

        // Note that emails are indexed lowercased because API searches explicitly do the same
        String expectedOutput = "{\"toEmail\":\"buyer@test.com\",\"toEmailAnonymous\":\"anonymous-buyer@test.com\",\"fromEmail\":\"seller@test.com\",\"fromEmailAnonymous\":\"anonymous-seller@test.com\",\"messageDirection\":\"SELLER_TO_BUYER\",\"messageState\":\"SENT\",\"humanResultState\":\"UNCHECKED\",\"receivedDate\":\"2012-01-30T19:01:52.000Z\",\"conversationStartDate\":\"2012-01-29T18:01:52.000Z\",\"messageText\":\"some text\",\"adId\":\"myAd#123\",\"attachments\":[],\"lastEditor\":null,\"customHeaders\":{},\"feedback\":[]}";
        assertEquals(expectedOutput, output);
    }

    private Conversation makeConversation(int messageCount) {
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
                .withPlainTextBody("some text");
    }
}