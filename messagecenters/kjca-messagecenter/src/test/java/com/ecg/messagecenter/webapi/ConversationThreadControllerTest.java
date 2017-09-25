package com.ecg.messagecenter.webapi;

import ca.kijiji.replyts.TextAnonymizer;
import com.ecg.messagecenter.listeners.UnreadCountCacher;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.UnreadCountCachePopulater;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.DefaultRiakSimplePostBoxRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

public class ConversationThreadControllerTest {
    @Mock
    private DefaultRiakSimplePostBoxRepository postBoxRepository = Mockito.mock(DefaultRiakSimplePostBoxRepository.class);

    @Mock
    private ConversationRepository conversationRepository = Mockito.mock(ConversationRepository.class);

    @Mock
    private RiakConversationBlockRepository conversationBlockRepository = Mockito.mock(RiakConversationBlockRepository.class);

    @Mock
    private MailCloakingService mailCloakingService = Mockito.mock(MailCloakingService.class);

    @Mock
    private TextAnonymizer textAnonymizer = Mockito.mock(TextAnonymizer.class);

    @Mock
    private UnreadCountCachePopulater unreadCountCachePopulater = Mockito.mock(UnreadCountCachePopulater.class);

    private ConversationThreadController controller;

    @Before
    public void setUp() throws Exception {
        controller = new ConversationThreadController(postBoxRepository, conversationRepository, conversationBlockRepository, mailCloakingService, textAnonymizer, unreadCountCachePopulater, 180);

        List<ConversationThread> conversationThreads = ImmutableList.of(new ConversationThread("ad", "conversationId", DateTime.now(), DateTime.now(), DateTime.now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("user@example.com"), Optional.empty()));
        PostBox postBox = new PostBox<>("user@example.com", conversationThreads, 100);
        Mockito.when(postBoxRepository.byId(PostBoxId.fromEmail("user@example.com"))).thenReturn(postBox);
    }

    @Test
    public void markConversationRead_triggersUnreadCountCacher() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        controller.getPostBoxConversationByEmailAndConversationId("user@example.com", "conversationId", false, request, new MockHttpServletResponse());

        Mockito.verify(unreadCountCachePopulater).populateCache("user@example.com");
    }

    @Test
    public void deleteSingleConversation_triggersUnreadCountCacher() throws Exception {
        controller.deleteSingleConversation("user@example.com", "conversationId", new MockHttpServletResponse());

        Mockito.verify(unreadCountCachePopulater).populateCache("user@example.com");
    }
}
