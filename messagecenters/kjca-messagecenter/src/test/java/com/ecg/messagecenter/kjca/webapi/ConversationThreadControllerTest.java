package com.ecg.messagecenter.kjca.webapi;

import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.kjca.persistence.block.CassandraConversationBlockRepository;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.kjca.persistence.ConversationThread;
import com.ecg.messagecenter.kjca.persistence.UnreadCountCachePopulater;
import com.ecg.messagecenter.kjca.sync.ConversationService;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

public class ConversationThreadControllerTest {
    private static final String EMAIL = "user@example.com";
    private static final String CONVERSATION_ID = "conversationId";

    private SimplePostBoxRepository postBoxRepository = Mockito.mock(SimplePostBoxRepository.class);
    private ConversationRepository conversationRepository = Mockito.mock(ConversationRepository.class);
    private CassandraConversationBlockRepository conversationBlockRepository = Mockito.mock(CassandraConversationBlockRepository.class);
    private MailCloakingService mailCloakingService = Mockito.mock(MailCloakingService.class);
    private TextAnonymizer textAnonymizer = Mockito.mock(TextAnonymizer.class);
    private UnreadCountCachePopulater unreadCountCachePopulater = Mockito.mock(UnreadCountCachePopulater.class);

    private ConversationThreadController controller;
    private PostBox<AbstractConversationThread> postBox;
    private PostBox<AbstractConversationThread> readPostBox;

    @Before
    public void setUp() throws Exception {
        final DateTime now = DateTime.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis()); // Freeze what Joda reports as "now".

        ConversationService conversationService = new ConversationService(postBoxRepository, conversationRepository, conversationBlockRepository, mailCloakingService, textAnonymizer, unreadCountCachePopulater);
        controller = new ConversationThreadController(conversationService, false);

        List<AbstractConversationThread> unreadConversationThreads = ImmutableList.of(new ConversationThread("ad", CONVERSATION_ID, now, now, now, true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EMAIL), Optional.empty()));
        List<AbstractConversationThread> readConversationThreads = ImmutableList.of(new ConversationThread("ad", CONVERSATION_ID, now, now, now, true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EMAIL), Optional.empty()));
        postBox = new PostBox<>(EMAIL, Optional.empty(), unreadConversationThreads);
        readPostBox = new PostBox<>(EMAIL, Optional.empty(), readConversationThreads);
        Mockito.when(postBoxRepository.byId(PostBoxId.fromEmail(EMAIL))).thenReturn(postBox);
    }

    @Test
    public void markConversationRead_triggersUnreadCountCacher() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        controller.readPostBox(EMAIL, CONVERSATION_ID, new MockHttpServletResponse());

        Mockito.verify(unreadCountCachePopulater).populateCache(readPostBox);
    }

    @Test
    public void deleteSingleConversation_triggersUnreadCountCacher() throws Exception {
        controller.deleteSingleConversation(EMAIL, CONVERSATION_ID);

        Mockito.verify(unreadCountCachePopulater).populateCache(postBox);
    }

    @After
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }
}
