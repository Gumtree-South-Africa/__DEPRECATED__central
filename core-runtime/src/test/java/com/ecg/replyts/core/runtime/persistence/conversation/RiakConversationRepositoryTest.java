package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakConversationRepositoryTest {
    private RiakConversationRepository repo;

    @Mock
    private ConversationBucket conversationBucket;

    @Mock
    private ConversationIndexBucket conversationIndexBucket;

    @Mock
    private ConversationSecretBucket secretRepo;

    @Mock
    private Conversation convToDelete;

    @Spy
    private ConversationCreatedEvent createdEvent = new ConversationCreatedEvent("convid", "adid", "buyerid", "sellerid", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap());

    @Before
    public void setUp() {
        repo = new RiakConversationRepository(secretRepo, conversationBucket, conversationIndexBucket);
        when(conversationBucket.byId("convid")).thenReturn(
                new ConversationEvents(Lists.<ConversationEvent>newArrayList(createdEvent)));
        when(convToDelete.getId()).thenReturn("delme");
        when(convToDelete.getBuyerSecret()).thenReturn("buyersecret");
        when(convToDelete.getSellerSecret()).thenReturn("sellersecret");
        when(convToDelete.getBuyerId()).thenReturn("someBuyer@somewhere.com");
        when(convToDelete.getSellerId()).thenReturn("someSeller@somewhere.com");
        when(convToDelete.getAdId()).thenReturn("adId");
    }

    @Test
    public void getConversationByIdBuildsNewConversation() {
        assertEquals("convid", repo.getById("convid").getId());
    }

    @Test
    public void getConversationByIdReturnsNullWhenConversationNotFound() {
        assertNull(repo.getById("nonexistamt"));
    }

    @Test
    public void findsConversationBySecret() {
        NewConversationCommand cmd = mock(NewConversationCommand.class);
        when(cmd.getConversationId()).thenReturn("convid");
        when(secretRepo.findConversationId("secretid")).thenReturn(cmd);
        assertEquals("convid", repo.getBySecret("secretid").getId());
    }

    @Test
    public void createsSecretsForNewConversations() {
        repo.commit("convid", Lists.<ConversationEvent>newArrayList(createdEvent));
        verify(secretRepo).persist(createdEvent);
        verify(conversationBucket).write("convid", Lists.<ConversationEvent>newArrayList(createdEvent));
    }

    @Test
    public void doesNotCreateSecretsForDoaConversations() {
        when(createdEvent.getState()).thenReturn(ConversationState.DEAD_ON_ARRIVAL);
        repo.commit("convid", Lists.<ConversationEvent>newArrayList(createdEvent));
        verifyZeroInteractions(secretRepo);
        verify(conversationBucket).write("convid", Lists.<ConversationEvent>newArrayList(createdEvent));
    }

    @Test
    public void deletesConversationWithSecrets() {
        repo.deleteConversation(convToDelete);
        verify(secretRepo).delete("buyersecret");
        verify(secretRepo).delete("sellersecret");
        verify(conversationBucket).delete("delme");
    }

    @Test
    public void doesNotDeleteConversationWhenDeleteSecretsFails() {
        doThrow(new RuntimeException()).when(secretRepo).delete(anyString());
        try {
            repo.deleteConversation(convToDelete);
        } catch (RuntimeException e) {
            // expected behaviour
        }
        verifyZeroInteractions(conversationBucket);
    }
}