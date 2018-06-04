package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse.IDHolder;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageSenderTest {
    @Mock
    private SearchService searchService;

    @Mock
    private ModerationService moderationService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private MutableConversationRepository conversationRepository;

    @Mock
    private Document2KafkaSink document2KafkaSink;

    @Mock
    private MutableConversation conv1;

    @Mock
    private MutableConversation conv2;

    @Mock
    private MutableConversation conv3;

    private MessageSender sender;

    @Before
    public void setUp() {
        when(searchService.search(any(SearchMessagePayload.class))).thenReturn(new RtsSearchResponse(Lists.newArrayList(
          new IDHolder("123a", "321a"),
          new IDHolder("123b", "321b"),
          new IDHolder("123c", "321c")
        )));

        when(applicationContext.getBean(ModerationService.class)).thenReturn(moderationService);

        when(conversationRepository.getById("321a")).thenReturn(conv1);
        when(conversationRepository.getById("321b")).thenReturn(conv2);
        when(conversationRepository.getById("321c")).thenReturn(conv3);

        sender = new MessageSender(applicationContext, searchService, document2KafkaSink, conversationRepository,
                4, 24, 20000);
    }

    @Test
    public void moderatesAllFoundMessagesToSent() throws MessageNotFoundException {
        sender.work();

        verify(moderationService).changeMessageState(conv1, "123a", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
        verify(moderationService).changeMessageState(conv2, "123b", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
        verify(moderationService).changeMessageState(conv3, "123c", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
    }

    @Test
    public void moderatesOtherMessagesIfOneFails() throws MessageNotFoundException {
        doThrow(new RuntimeException()).when(moderationService).changeMessageState(conv1, "123a", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));

        sender.work();

        verify(moderationService).changeMessageState(conv1, "123a", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
        verify(moderationService).changeMessageState(conv2, "123b", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
        verify(moderationService).changeMessageState(conv3, "123c", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
    }

    @Test
    public void updateSearchAsyncIfMessageNotFoundException() throws MessageNotFoundException {
        doThrow(new MessageNotFoundException("")).when(moderationService).changeMessageState(conv1, "123a", new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));

        sender.work();

        verify(document2KafkaSink, times(1)).pushToKafka(conv1);
    }
}
