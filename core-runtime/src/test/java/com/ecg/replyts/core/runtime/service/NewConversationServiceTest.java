package com.ecg.replyts.core.runtime.service;

import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class NewConversationServiceTest {

    @Mock
    private Guids guids;
    @Mock
    private UniqueConversationSecret secret;
    @Mock
    private MutableConversationRepository conversationRepository;

    @InjectMocks
    private NewConversationService newConversationService;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        newConversationService = new NewConversationService(guids, secret, conversationRepository);

        assertNotNull(newConversationService);
    }

    @After
    public void tearDown() throws Exception {
        reset(secret, guids, conversationRepository);
    }

    @Test
    public void testGenerateGuid() throws Exception {

        String randomGuid = "123abc";
        when(guids.nextGuid()).thenReturn(randomGuid);

        String returnedGuid = newConversationService.nextGuid();

        assertEquals(randomGuid, returnedGuid);

        verify(guids).nextGuid();
    }

    @Test
    public void testGenerateSecret() throws Exception {

        String randomSecret = "123abc";
        when(secret.nextSecret()).thenReturn(randomSecret);

        String returnedSecret = newConversationService.nextSecret();

        assertEquals(randomSecret, returnedSecret);

        verify(secret).nextSecret();
    }

    @Test
    public void testCommitConversation() throws Exception {

        String convId = "convId";
        String adId = "adId";
        String buyerEmail = "buyerEmail@marktplaats.nl";
        String sellerEmail = "buyerEmail@marktplaats.nl";
        ConversationState convState = ConversationState.ACTIVE;

        ArgumentCaptor<List<ConversationEvent>> listCaptor = ArgumentCaptor.forClass((Class) List.class);

        newConversationService.commitConversation(convId, adId, buyerEmail, sellerEmail, convState);

        verify(conversationRepository).commit(eq(convId), listCaptor.capture());

        List<ConversationEvent> conversationEvents = listCaptor.getValue();

        assertNotNull(conversationEvents);
        assertEquals(1, conversationEvents.size());
        assertTrue(conversationEvents.get(0) instanceof ConversationCreatedEvent);
        assertEquals(convId, ((ConversationCreatedEvent) conversationEvents.get(0)).getConversationId());
        assertEquals(adId, ((ConversationCreatedEvent) conversationEvents.get(0)).getAdId());
        assertEquals(buyerEmail, ((ConversationCreatedEvent) conversationEvents.get(0)).getBuyerId());
        assertEquals(sellerEmail, ((ConversationCreatedEvent) conversationEvents.get(0)).getSellerId());
        assertEquals(convState, ((ConversationCreatedEvent) conversationEvents.get(0)).getState());
    }


}