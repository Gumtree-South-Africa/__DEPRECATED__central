package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.bucket.FetchBucket;
import com.basho.riak.client.bucket.WriteBucket;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RiakConversationRepositoryTest {
    private TestRiakConversationRepository riakConversationRepository;

    @Mock
    private FetchBucket fetchBucket;

    @Mock
    private WriteBucket writeBucket;

    @Mock
    private Bucket bucket;

    @Mock
    private ConversationBucket conversationBucket;

    @Mock
    private ConversationIndexBucket conversationIndexBucket;

    @Mock
    private ConversationSecretBucket conversationSecretBucket;

    @Mock
    private IRiakClient riakClient;

    @Mock
    private Conversation conversationToDelete;

    @Spy
    private ConversationCreatedEvent createdEvent = new ConversationCreatedEvent("convid", "adid", "buyerid", "sellerid", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap());

    @Before
    public void setUp() throws Exception {
        setUpRiak();

        riakConversationRepository = new TestRiakConversationRepository(conversationBucket, conversationIndexBucket, conversationSecretBucket);

        when(conversationBucket.byId("convid")).thenReturn(new ConversationEvents(Lists.newArrayList(createdEvent)));
        when(conversationToDelete.getId()).thenReturn("delme");
        when(conversationToDelete.getBuyerSecret()).thenReturn("buyersecret");
        when(conversationToDelete.getSellerSecret()).thenReturn("sellersecret");
        when(conversationToDelete.getBuyerId()).thenReturn("someBuyer@somewhere.com");
        when(conversationToDelete.getSellerId()).thenReturn("someSeller@somewhere.com");
        when(conversationToDelete.getAdId()).thenReturn("adId");
    }

    private void setUpRiak() throws RiakRetryFailedException {
        when(riakClient.fetchBucket(anyString())).thenReturn(fetchBucket);
        when(fetchBucket.withRetrier(any())).thenReturn(fetchBucket);
        when(fetchBucket.execute()).thenReturn(bucket);
        when(riakClient.updateBucket(any())).thenReturn(writeBucket);
        when(writeBucket.allowSiblings(anyBoolean())).thenReturn(writeBucket);
        when(writeBucket.lastWriteWins(anyBoolean())).thenReturn(writeBucket);
        when(writeBucket.execute()).thenReturn(bucket);
    }

    @Test
    public void getConversationByIdBuildsNewConversation() {
        assertEquals("convid", riakConversationRepository.getById("convid").getId());
    }

    @Test
    public void getConversationByIdReturnsNullWhenConversationNotFound() {
        assertNull(riakConversationRepository.getById("nonexistamt"));
    }

    @Test
    public void getConversationEvents() {
        when(conversationBucket.byId("foo")).thenReturn(null);
        assertNull(riakConversationRepository.getConversationEvents("foo"));
    }

    @Test
    public void getConversationEventsNotEmpty() {
        ConversationEvents events = new ConversationEvents(Collections.singletonList(createdEvent));
        when(conversationBucket.byId("foo")).thenReturn(events);
        assertEquals(events.getEvents(), riakConversationRepository.getConversationEvents("foo"));
    }

    @Test
    public void findsConversationBySecret() {
        NewConversationCommand cmd = mock(NewConversationCommand.class);
        when(cmd.getConversationId()).thenReturn("convid");
        when(conversationSecretBucket.findConversationId("secretid")).thenReturn(cmd);
        assertEquals("convid", riakConversationRepository.getBySecret("secretid").getId());
    }

    @Test
    public void createsSecretsForNewConversations() {
        riakConversationRepository.commit("convid", Lists.newArrayList(createdEvent));
        verify(conversationSecretBucket).persist(createdEvent);
        verify(conversationBucket).write("convid", Lists.newArrayList(createdEvent));
    }

    @Test
    public void doesNotCreateSecretsForDoaConversations() {
        when(createdEvent.getState()).thenReturn(ConversationState.DEAD_ON_ARRIVAL);
        riakConversationRepository.commit("convid", Lists.newArrayList(createdEvent));
        verifyZeroInteractions(conversationSecretBucket);
        verify(conversationBucket).write("convid", Lists.newArrayList(createdEvent));
    }

    @Test
    public void deletesConversationWithSecrets() {
        riakConversationRepository.deleteConversation(conversationToDelete);
        verify(conversationSecretBucket).delete("buyersecret");
        verify(conversationSecretBucket).delete("sellersecret");
        verify(conversationBucket).delete("delme");
    }

    @Test
    public void doesNotDeleteConversationWhenDeleteSecretsFails() {
        doThrow(new RuntimeException()).when(conversationSecretBucket).delete(anyString());
        try {
            riakConversationRepository.deleteConversation(conversationToDelete);
        } catch (RuntimeException e) {
            // expected behaviour
        }
        verifyZeroInteractions(conversationBucket);
    }

    private class TestRiakConversationRepository extends RiakConversationRepository {
        TestRiakConversationRepository(ConversationBucket conversationBucket, ConversationIndexBucket indexBucket, ConversationSecretBucket secretBucket) {
            super(riakClient, "", true, true);
            this.conversationBucket = conversationBucket;
            this.conversationSecretBucket = secretBucket;
            this.conversationIndexBucket = indexBucket;
        }
    }
}