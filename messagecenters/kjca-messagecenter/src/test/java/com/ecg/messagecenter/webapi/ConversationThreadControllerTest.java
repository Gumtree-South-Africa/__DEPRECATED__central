package com.ecg.messagecenter.webapi;

import ca.kijiji.replyts.TextAnonymizer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
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
    private static final String EMAIL = "user@example.com";
    private static final String CONVERSATION_ID = "conversationId";

    private DefaultRiakSimplePostBoxRepository postBoxRepository = Mockito.mock(DefaultRiakSimplePostBoxRepository.class);
    private ConversationRepository conversationRepository = Mockito.mock(ConversationRepository.class);
    private RiakConversationBlockRepository conversationBlockRepository = Mockito.mock(RiakConversationBlockRepository.class);
    private MailCloakingService mailCloakingService = Mockito.mock(MailCloakingService.class);
    private TextAnonymizer textAnonymizer = Mockito.mock(TextAnonymizer.class);
    private UnreadCountCachePopulater unreadCountCachePopulater = Mockito.mock(UnreadCountCachePopulater.class);

    private ConversationThreadController controller;
    private PostBox<AbstractConversationThread> postBox;

    @Before
    public void setUp() throws Exception {
        controller = new ConversationThreadController(postBoxRepository, conversationRepository, conversationBlockRepository, mailCloakingService, textAnonymizer, unreadCountCachePopulater);

        List<AbstractConversationThread> conversationThreads = ImmutableList.of(new ConversationThread("ad", CONVERSATION_ID, DateTime.now(), DateTime.now(), DateTime.now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EMAIL), Optional.empty()));
        postBox = new PostBox<>(EMAIL, Optional.empty(), conversationThreads);
        Mockito.when(postBoxRepository.byId(PostBoxId.fromEmail(EMAIL))).thenReturn(postBox);
    }

    @Test
    public void markConversationRead_triggersUnreadCountCacher() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        controller.getPostBoxConversationByEmailAndConversationId(EMAIL, CONVERSATION_ID, false, request, new MockHttpServletResponse());

        Mockito.verify(unreadCountCachePopulater).populateCache(postBox);
    }

    @Test
    public void deleteSingleConversation_triggersUnreadCountCacher() throws Exception {
        controller.deleteSingleConversation(EMAIL, CONVERSATION_ID, new MockHttpServletResponse());

        Mockito.verify(unreadCountCachePopulater).populateCache(postBox);
    }
}
