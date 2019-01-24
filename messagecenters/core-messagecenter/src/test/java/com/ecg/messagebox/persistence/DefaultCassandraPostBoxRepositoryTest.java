package com.ecg.messagebox.persistence;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.model.ConversationIndex;
import com.ecg.replyts.core.runtime.cluster.Guids;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCassandraPostBoxRepositoryTest {

    @Test
    public void conversationThreadComparator_shouldSortOnLastMessageId_thenSortOnConversationId_descending() {

        Set<ConversationThread> sorted = new TreeSet<>(DefaultCassandraMessageBoxRepository.CONVERSATION_THREAD_COMPARATOR);

        ConversationThread conversationThread1 = createConversationThread(Guids.next(), UUIDs.timeBased());
        ConversationThread conversationThread2 = createConversationThread(Guids.next(), null);
        ConversationThread conversationThread3 = createConversationThread(Guids.next(), null);
        ConversationThread conversationThread4 = createConversationThread(Guids.next(), UUIDs.timeBased());
        ConversationThread conversationThread5 = createConversationThread(Guids.next(), null);
        ConversationThread conversationThread6 = createConversationThread(Guids.next(), UUIDs.timeBased());

        sorted.addAll(Arrays.asList(conversationThread1, conversationThread2, conversationThread3, conversationThread4, conversationThread5, conversationThread6));

        assertThat(sorted).containsExactly(
            conversationThread5,
            conversationThread3,
            conversationThread2,
            conversationThread6,
            conversationThread4,
            conversationThread1);
    }

    private ConversationThread createConversationThread(String conversationId, UUID lastMessageId) {
        Message lastMessage = lastMessageId != null ? createMessage(lastMessageId) : null;
        return new ConversationThread(conversationId, "adId", "userId", Visibility.ACTIVE, null, null, lastMessage, null);
    }

    @Test
    public void conversationIndexComparatorr_shouldSortOnLastMessageId_thenSortOnConversationId_descending() {

        Set<ConversationIndex> sorted = new TreeSet<>(DefaultCassandraMessageBoxRepository.CONVERSATION_INDEX_COMPARATOR);

        ConversationIndex conversationIndex1 = createConversationIndex(Guids.next(), UUIDs.timeBased());
        ConversationIndex conversationIndex2 = createConversationIndex(Guids.next(), UUIDs.timeBased());
        ConversationIndex conversationIndex3 = createConversationIndex(Guids.next(), null);
        ConversationIndex conversationIndex4 = createConversationIndex(Guids.next(), null);
        ConversationIndex conversationIndex5 = createConversationIndex(Guids.next(), null);
        ConversationIndex conversationIndex6 = createConversationIndex(Guids.next(), UUIDs.timeBased());

        sorted.addAll(Arrays.asList(conversationIndex1, conversationIndex2, conversationIndex3, conversationIndex4, conversationIndex5, conversationIndex6));

        assertThat(sorted).containsExactly(
            conversationIndex5,
            conversationIndex4,
            conversationIndex3,
            conversationIndex6,
            conversationIndex2,
            conversationIndex1);
    }

    private ConversationIndex createConversationIndex(String conversationId, UUID lastMessageId) {
        return new ConversationIndex(conversationId, "adId", Visibility.ACTIVE, lastMessageId);
    }

    @Test
    public void messageComparatorr_shouldSortOnId_ascending() {

        Set<Message> sorted = new TreeSet<>(DefaultCassandraMessageBoxRepository.MESSAGE_COMPARATOR);

        Message message1 = createMessage(UUIDs.timeBased());
        Message message2 = createMessage(UUIDs.timeBased());
        Message message3 = createMessage(UUIDs.timeBased());

        sorted.addAll(Arrays.asList(message3, message1, message2));

        assertThat(sorted).containsExactly(
            message1,
            message2,
            message3);
    }

    private Message createMessage(UUID lastMessageId) {
        return new Message(lastMessageId, MessageType.CHAT, null);
    }
}
