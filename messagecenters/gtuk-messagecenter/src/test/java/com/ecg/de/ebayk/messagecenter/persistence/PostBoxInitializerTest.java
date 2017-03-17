package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostBoxInitializerTest {

    private PostBoxRepository repository;
    private PostBoxInitializer postBoxInitializer;
    private PostBox postBox;
    private Counter counter;
    private PostBoxInitializer.PostBoxWriteCallback callback;

    @Before
    public void setup() {
        repository = mock(PostBoxRepository.class);
        postBoxInitializer = new PostBoxInitializer(repository, 250, 10);
        postBox = mock(PostBox.class);
        counter = mock(Counter.class);
        callback = mock(PostBoxInitializer.PostBoxWriteCallback.class);
    }

    @Test
    public void testConversationThreadReceivedAtDateIsSetToDateOfLastMessageReceived() {
        DateTime receivedDate = DateTime.parse("2016-01-01T12:00:00Z");
        DateTime lastModifiedDate = receivedDate.plusDays(3);

        Message message = mockMessage(receivedDate, MessageState.SENT);
        Conversation conversation = mockConversation(Lists.newArrayList(message), lastModifiedDate);

        when(repository.byId(anyString())).thenReturn(postBox);
        when(postBox.getNewRepliesCounter()).thenReturn(counter);

        postBoxInitializer.moveConversationToPostBox("email@address.com", conversation, true, callback);
        ArgumentCaptor<PostBox> postBoxArgumentCaptor = ArgumentCaptor.forClass(PostBox.class);
        verify(repository).write(postBoxArgumentCaptor.capture());

        ConversationThread thread = postBoxArgumentCaptor.getValue().getConversationThreads().iterator().next();

        assertThat(thread.getReceivedAt(), equalTo(receivedDate));
    }

    @Test
    public void testConversationThreadReceivedAtDateIsSetToDateOfLastSentMessageReceived() {
        DateTime receivedDate = DateTime.parse("2016-01-01T12:00:00Z");
        DateTime lastModifiedDate = receivedDate.plusDays(3);

        List<Message> messages = Lists.newArrayList();
        messages.add(mockMessage(receivedDate, MessageState.SENT));
        messages.add(mockMessage(receivedDate.plusDays(1), MessageState.BLOCKED));
        messages.add(mockMessage(receivedDate.plusDays(2), MessageState.IGNORED));
        messages.add(mockMessage(receivedDate.plusDays(3), MessageState.DISCARDED));
        messages.add(mockMessage(receivedDate.plusDays(4), MessageState.HELD));
        Conversation conversation = mockConversation(messages, lastModifiedDate);

        when(repository.byId(anyString())).thenReturn(postBox);
        when(postBox.getNewRepliesCounter()).thenReturn(counter);

        postBoxInitializer.moveConversationToPostBox("email@address.com", conversation, true, callback);
        ArgumentCaptor<PostBox> postBoxArgumentCaptor = ArgumentCaptor.forClass(PostBox.class);
        verify(repository).write(postBoxArgumentCaptor.capture());

        ConversationThread thread = postBoxArgumentCaptor.getValue().getConversationThreads().iterator().next();

        assertThat(thread.getReceivedAt(), equalTo(receivedDate));
    }


    @Test
    public void testNoThreadsAddingToConversationIfNoMessagePresent() {
        DateTime receivedDate = DateTime.parse("2016-01-01T12:00:00Z");
        DateTime lastModifiedDate = receivedDate.plusDays(3);

        Conversation conversation = mockConversation(Lists.newArrayList(), lastModifiedDate);

        when(repository.byId(anyString())).thenReturn(postBox);
        when(postBox.getNewRepliesCounter()).thenReturn(counter);

        postBoxInitializer.moveConversationToPostBox("email@address.com", conversation, true, callback);
        verify(repository, times(0)).write(any(PostBox.class));
    }

    private Message mockMessage(DateTime receivedDate, MessageState state) {
        Message message = mock(Message.class);
        when(message.getReceivedAt()).thenReturn(receivedDate);
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getPlainTextBody()).thenReturn("A message to you");
        when(message.getState()).thenReturn(state);
        return message;
    }

    private Conversation mockConversation(List<Message> messages, DateTime lastModifiedDate) {
        Conversation conversation = mock(Conversation.class);
        when(conversation.isClosedBy(any(ConversationRole.class))).thenReturn(false);
        when(conversation.getMessages()).thenReturn(messages);
        when(conversation.getBuyerId()).thenReturn("email@address.com");
        when(conversation.getSellerId()).thenReturn("seller@email.com");
        when(conversation.getAdId()).thenReturn("123456");
        when(conversation.getId()).thenReturn("314159265");
        when(conversation.getCreatedAt()).thenReturn(DateTime.now());
        when(conversation.getLastModifiedAt()).thenReturn(lastModifiedDate);
        return conversation;
    }

}