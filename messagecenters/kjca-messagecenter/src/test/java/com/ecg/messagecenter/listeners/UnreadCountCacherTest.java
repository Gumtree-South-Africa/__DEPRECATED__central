package com.ecg.messagecenter.listeners;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnreadCountCacherTest {
    private static final String UNREAD_COUNT_CACHE_QUEUE = "unreadCountCacheQueue";
    private static final String USER_EMAIL = "user@example.com";
    private static final String OTHER_USER_EMAIL = "other.user@example.com";

    @Mock
    private final SimplePostBoxRepository postBoxRepository = mock(SimplePostBoxRepository.class);

    @Mock
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);

    private UnreadCountCacher unreadCountCacher;

    @Before
    public void setUp() throws Exception {
        unreadCountCacher = new UnreadCountCacher(this.postBoxRepository, this.jmsTemplate, UNREAD_COUNT_CACHE_QUEUE);
    }

    @Test
    public void simplePath() throws Exception {
        final List<AbstractConversationThread> conversations = new ArrayList<>(3);
        final DateTime now = DateTime.now();
        conversations.add(new ConversationThread("a", "b", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("c", "d", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("e", "f", now, now, now, true, empty(), empty(), empty(), Optional.of(OTHER_USER_EMAIL), empty()));

        when(postBoxRepository.byId(PostBoxId.fromEmail(USER_EMAIL))).thenReturn(new PostBox<AbstractConversationThread>(USER_EMAIL, conversations, 30));

        unreadCountCacher.success(USER_EMAIL, 3L, true);

        verify(this.jmsTemplate).convertAndSend(UNREAD_COUNT_CACHE_QUEUE, "{\"email\":\"user@example.com\",\"total\":3,\"asPoster\":1,\"asReplier\":2}");
    }

    @Test
    public void skipsReadConversations() throws Exception {
        final List<AbstractConversationThread> conversations = new ArrayList<>(3);
        final DateTime now = DateTime.now();
        conversations.add(new ConversationThread("a", "b", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("c", "d", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("e", "f", now, now, now, true, empty(), empty(), empty(), Optional.of(OTHER_USER_EMAIL), empty()));
        conversations.add(new ConversationThread("g", "h", now, now, now, false, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));

        when(postBoxRepository.byId(PostBoxId.fromEmail(USER_EMAIL))).thenReturn(new PostBox<AbstractConversationThread>(USER_EMAIL, conversations, 30));

        unreadCountCacher.success(USER_EMAIL, 3L, true);

        verify(this.jmsTemplate).convertAndSend(UNREAD_COUNT_CACHE_QUEUE, "{\"email\":\"user@example.com\",\"total\":3,\"asPoster\":1,\"asReplier\":2}");
    }

    @Test
    public void absorbsJmsErrors() throws Exception {
        final List<AbstractConversationThread> conversations = new ArrayList<>(3);
        final DateTime now = DateTime.now();
        conversations.add(new ConversationThread("a", "b", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("c", "d", now, now, now, true, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));
        conversations.add(new ConversationThread("e", "f", now, now, now, true, empty(), empty(), empty(), Optional.of(OTHER_USER_EMAIL), empty()));
        conversations.add(new ConversationThread("g", "h", now, now, now, false, empty(), empty(), empty(), Optional.of(USER_EMAIL), empty()));

        when(postBoxRepository.byId(PostBoxId.fromEmail(USER_EMAIL))).thenReturn(new PostBox<AbstractConversationThread>(USER_EMAIL, conversations, 30));
        doThrow(new MessageConversionException("Couldn't make an ActiveMQ Message")).when(this.jmsTemplate).convertAndSend(UNREAD_COUNT_CACHE_QUEUE, "{\"email\":\"user@example.com\",\"total\":3,\"asPoster\":1,\"asReplier\":2}");

        unreadCountCacher.success(USER_EMAIL, 3L, true);

        assertTrue("Made it out of the callback successfully.", true);
    }
}
