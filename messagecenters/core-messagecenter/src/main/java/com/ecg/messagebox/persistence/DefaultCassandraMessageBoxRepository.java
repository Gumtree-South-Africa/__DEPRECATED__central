package com.ecg.messagebox.persistence;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.ecg.messagebox.resources.requests.PartnerMessagePayload;
import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.model.ConversationIndex;
import com.ecg.messagebox.persistence.model.PaginatedConversationIds;
import com.ecg.messagebox.persistence.model.UnreadCounts;
import com.ecg.messagebox.util.StreamUtils;
import com.ecg.messagebox.util.uuid.UUIDComparator;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;
import static com.ecg.messagebox.model.Visibility.get;
import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Component
public class DefaultCassandraMessageBoxRepository implements CassandraMessageBoxRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCassandraMessageBoxRepository.class);

    static final Comparator<ConversationThread> CONVERSATION_THREAD_COMPARATOR = comparing(
        ConversationThread::getLatestMessage,
        nullsLast(comparing(Message::getId, UUIDComparator::staticCompare))).thenComparing(ConversationThread::getId).reversed();

    static final Comparator<ConversationIndex> CONVERSATION_INDEX_COMPARATOR = comparing(
        ConversationIndex::getLatestMessageId,
        nullsLast(UUIDComparator::staticCompare)).thenComparing(ConversationIndex::getConversationId).reversed();

    static final Comparator<Message> MESSAGE_COMPARATOR = comparing(Message::getId, UUIDComparator::staticCompare);

    private final CassandraTemplate cassandraTemplate;

    @Autowired
    public DefaultCassandraMessageBoxRepository(CassandraTemplate cassandraTemplate) {
        this.cassandraTemplate = cassandraTemplate;
    }

    @Override
    public PostBox getPostBox(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        PaginatedConversationIds paginatedConversationIds = getPaginatedConversationIds(userId, visibility, conversationsOffset, conversationsLimit);
        ResultSet resultSet = cassandraTemplate.execute(Statements.SELECT_CONVERSATIONS, userId, paginatedConversationIds.getConversationIds());

        Map<String, UnreadCounts> conversationUnreadCountsMap = getConversationUnreadCountMap(userId);
        List<ConversationThread> conversations = StreamUtils.toStream(resultSet)
                .map(row -> {
                    ConversationThread conversation = createConversation(userId, row);
                    for (Participant participant : conversation.getParticipants()) {
                        if (userId.equals(participant.getUserId())) {
                            conversation.addNumUnreadMessages(userId, conversationUnreadCountsMap.getOrDefault(row.getString("convid"),
                                    new UnreadCounts(0, 0)).getUserUnreadCounts());
                        } else {
                            conversation.addNumUnreadMessages(participant.getUserId(), conversationUnreadCountsMap.getOrDefault(row.getString("convid"),
                                    new UnreadCounts(0, 0)).getOtherParticipantUnreadCount());
                        }
                    }
                    return conversation;
                }).sorted(CONVERSATION_THREAD_COMPARATOR)
                .collect(toList());

        UserUnreadCounts userUnreadCounts = createUserUnreadCounts(userId, conversationUnreadCountsMap.values().stream()
                .map(UnreadCounts::getUserUnreadCounts).collect(toList()));

        return new PostBox(userId, conversations, userUnreadCounts, paginatedConversationIds.getConversationsTotalCount());
    }

    public PaginatedConversationIds getPaginatedConversationIds(String userId, Visibility visibility, int offset, int limit) {
        ResultSet resultSet = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_INDICES, userId);

        List<String> allConversationIds = StreamUtils.toStream(resultSet)
                .map(row -> new ConversationIndex(row.getString("convid"), row.getString("adid"), get(row.getInt("vis")), row.getUUID("latestmsgid")))
                .filter(ci -> ci.getVisibility() == visibility)
                .sorted(CONVERSATION_INDEX_COMPARATOR)
                .map(ConversationIndex::getConversationId)
                .collect(toList());

        List<String> paginatedConversationIds = allConversationIds.stream()
                .skip(offset * limit)
                .limit(limit)
                .collect(toList());

        return new PaginatedConversationIds(paginatedConversationIds, allConversationIds.size());
    }

    private Map<String, UnreadCounts> getConversationUnreadCountMap(String userId) {
        ResultSet resultSet = cassandraTemplate.execute(Statements.SELECT_CONVERSATIONS_UNREAD_COUNTS, userId);
        return StreamUtils.toStream(resultSet)
                .collect(Collectors.toMap(
                        row -> row.getString("convid"),
                        row -> new UnreadCounts(row.getInt("unread"), row.getInt("unreadother")))
                );
    }

    @Override
    public Optional<MessageNotification> getConversationMessageNotification(String userId, String conversationId) {
        Row row = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_MESSAGE_NOTIFICATION, userId, conversationId).one();
        return row != null ? of(MessageNotification.get(row.getInt("ntfynew"))) : empty();
    }

    @Override
    public Optional<ConversationThread> getConversationWithMessages(String userId, String conversationId, String messageIdCursor, int messagesLimit) {
        LOG.trace("Retrieving conversationThread for conversationId {}, userId {}", conversationId, userId);
        Row row = cassandraTemplate.execute(Statements.SELECT_CONVERSATION, userId, conversationId).one();
        if (row != null) {
            ConversationThread conversation = createConversation(userId, row);

            for (Participant participant : conversation.getParticipants()) {
                int unreadMessagesCount = getConversationUnreadCount(participant.getUserId(), conversationId);
                conversation.addNumUnreadMessages(participant.getUserId(), unreadMessagesCount);
            }

            List<Message> messages = getConversationMessages(userId, conversationId, messageIdCursor, messagesLimit);
            conversation.addMessages(messages);

            return of(conversation);
        } else {
            LOG.trace("Could not get conversationThread for conversationId {}, userId {}", conversationId, userId);
            return empty();
        }
    }

    List<Message> getConversationMessages(String userId, String conversationId, String messageIdCursor, int limit) {
        if (limit < 1) {
            return Collections.emptyList();
        }
        ResultSet resultSet;
        if (messageIdCursor != null) {
            resultSet = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITH_CURSOR, userId, conversationId, UUID.fromString(messageIdCursor), limit);
        } else {
            resultSet = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR, userId, conversationId, limit);
        }

        return StreamUtils.toStream(resultSet)
                .map(row -> createMessage(userId, conversationId, row.getUUID("msgid"), row.getString("metadata"), row.getString("type")))
                .sorted(MESSAGE_COMPARATOR)
                .collect(toList());
    }

    private Message createMessage(String userId, String conversationId, UUID messageId, String metadata, String type) {
        return new Message(messageId, MessageType.get(type), JsonConverter.fromMessageMetadataJson(userId, conversationId, messageId.toString(), metadata));
    }

    private ConversationThread createConversation(String userId, Row row) {
        String conversationId = row.getString("convid");
        String adId = row.getString("adid");

        Visibility visibility = get(row.getInt("vis"));
        MessageNotification messageNotification = MessageNotification.get(row.getInt("ntfynew"));
        List<Participant> participants = JsonConverter.fromParticipantsJson(userId, conversationId, row.getString("participants"));
        Message latestMessage = JsonConverter.fromMessageJson(userId, conversationId, row.getString("latestmsg"));
        ConversationMetadata metadata = JsonConverter.fromConversationMetadataJson(userId, conversationId, row.getString("metadata"));

        return new ConversationThread(conversationId, adId, userId, visibility, messageNotification, participants, latestMessage, metadata);
    }

    @Override
    public UserUnreadCounts getUserUnreadCounts(String userId) {
        ResultSet result = cassandraTemplate.execute(Statements.SELECT_USER_UNREAD_COUNTS, userId);
        List<Integer> conversationUnreadCounts = StreamUtils.toStream(result)
                .map(row -> row.getInt("unread"))
                .collect(toList());
        return createUserUnreadCounts(userId, conversationUnreadCounts);
    }

    private UserUnreadCounts createUserUnreadCounts(String userId, Collection<Integer> conversationUnreadCounts) {
        CountersConsumer counters = conversationUnreadCounts.stream()
                .collect(CountersConsumer::new, CountersConsumer::add, CountersConsumer::reduce);
        return new UserUnreadCounts(userId, counters.numUnreadConversations, counters.numUnreadMessages);
    }

    private static class CountersConsumer {
        private int numUnreadConversations = 0;
        private int numUnreadMessages = 0;

        void add(int numUnreadMessagesInConversation) {
            numUnreadConversations += numUnreadMessagesInConversation > 0 ? 1 : 0;
            numUnreadMessages += numUnreadMessagesInConversation;
        }

        void reduce(CountersConsumer countersConsumer) {
            numUnreadConversations += countersConsumer.numUnreadConversations;
            numUnreadMessages += countersConsumer.numUnreadMessages;
        }
    }

    @Override
    public int getConversationUnreadCount(String userId, String conversationId) {
        Row unreadCountResult = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_UNREAD_COUNT, userId, conversationId).one();
        return unreadCountResult == null ? 0 : unreadCountResult.getInt("unread");
    }

    @Override
    public int getConversationOtherParticipantUnreadCount(String userId, String conversationId) {
        Row unreadCountResult = cassandraTemplate.execute(Statements.SELECT_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT, userId, conversationId).one();
        return unreadCountResult == null ? 0 : unreadCountResult.getInt("unreadother");
    }

    @Override
    public void resetConversationUnreadCount(String userId, String otherParticipantUserId, String conversationId, String adId) {
        BatchStatement batch = new BatchStatement();
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_UNREAD_COUNT, 0, userId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT, 0, otherParticipantUserId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT, 0, userId, adId, conversationId));
        cassandraTemplate.execute(batch);
    }

    @Override
    public void resetConversationsUnreadCount(PostBox postBox) {
        BatchStatement batch = new BatchStatement();

        for (ConversationThread conversation : postBox.getConversations()) {
            if (conversation.getNumUnreadMessages(postBox.getUserId()) > 0) {
                batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_UNREAD_COUNT, 0, postBox.getUserId(), conversation.getId()));
                batch.add(cassandraTemplate.bind(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT, 0, postBox.getUserId(), conversation.getAdId(), conversation.getId()));

                List<Participant> participants = conversation.getParticipants();
                String otherParticipantUserId = participants.get(0).getUserId().equals(postBox.getUserId()) ? participants.get(1).getUserId() : participants.get(0).getUserId();
                batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT, 0, otherParticipantUserId, conversation.getId()));
            }
        }

        if (!batch.getStatements().isEmpty()) {
            cassandraTemplate.execute(batch);
        }
    }

    @Override
    public void createConversation(String userId, ConversationThread conversation, Message message, boolean incrementUnreadCount) {
        BatchStatement batch = new BatchStatement();

        String conversationId = conversation.getId();
        String adId = conversation.getAdId();

        updateConversation(batch, userId, conversationId, adId, message, conversation.getParticipants(), conversation.getMetadata());
        updateAdConversationIndex(batch, userId, conversationId, adId, message.getId());
        insertMessage(batch, userId, conversationId, message);
        if (incrementUnreadCount) {
            updateConversationUnreadCount(batch, userId, conversationId, adId);
            updateOtherParticipantConversationUnreadCount(batch, conversationId, message.getSenderUserId());
        }
        insertAdConversationModificationIdx(batch, userId, conversationId, adId, message.getId());

        cassandraTemplate.execute(batch);
    }

    @Override
    public void createEmptyConversation(String userId, ConversationThread conversation) {
        BatchStatement batch = new BatchStatement();

        String conversationId = conversation.getId();
        String adId = conversation.getAdId();

        updateConversation(batch, userId, conversationId, adId, null, conversation.getParticipants(), conversation.getMetadata());
        updateAdConversationIndex(batch, userId, conversationId, adId, null);

        cassandraTemplate.execute(batch);
    }

    @Override
    public void createPartnerConversation(PartnerMessagePayload payload, Message message, String conversationId, String userId, boolean incrementUnreadCount) {
        BatchStatement batch = new BatchStatement();

        String adId = payload.getAdId();
        List<Participant> participants = Arrays.asList(payload.getBuyer(), payload.getSeller());
        ConversationMetadata metadata = new ConversationMetadata(DateTime.now(), payload.getSubject(), payload.getAdTitle(), null);

        updateConversation(batch, userId, conversationId, adId, message, participants, metadata);
        updateAdConversationIndex(batch, userId, conversationId, adId, message.getId());
        insertMessage(batch, userId, conversationId, message);
        if (incrementUnreadCount) {
            updateConversationUnreadCount(batch, userId, conversationId, adId);
            updateOtherParticipantConversationUnreadCount(batch, conversationId, message.getSenderUserId());
        }
        insertAdConversationModificationIdx(batch, userId, conversationId, adId, message.getId());

        cassandraTemplate.execute(batch);
    }

    @Override
    public void addMessage(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        BatchStatement batch = new BatchStatement();

        updateConversationLatestMessage(batch, userId, conversationId, message);
        updateAdConversationIndex(batch, userId, conversationId, adId, message.getId());
        insertMessage(batch, userId, conversationId, message);
        if (incrementUnreadCount) {
            updateConversationUnreadCount(batch, userId, conversationId, adId);
            updateOtherParticipantConversationUnreadCount(batch, conversationId, message.getSenderUserId());
        }
        insertAdConversationModificationIdx(batch, userId, conversationId, adId, message.getId());

        cassandraTemplate.execute(batch);
    }

    @Override
    public void addSystemMessage(String userId, String conversationId, String adId, Message message) {
        BatchStatement batch = new BatchStatement();

        updateConversationLatestMessage(batch, userId, conversationId, message);
        updateAdConversationIndex(batch, userId, conversationId, adId, message.getId());
        insertMessage(batch, userId, conversationId, message);
        updateConversationUnreadCount(batch, userId, conversationId, adId);
        insertAdConversationModificationIdx(batch, userId, conversationId, adId, message.getId());

        cassandraTemplate.execute(batch);
    }

    private void updateConversation(BatchStatement batch, String userId, String conversationId, String adId, Message message,
            List<Participant> participants, ConversationMetadata metadata) {
        String participantsJson = JsonConverter.toParticipantsJson(userId, conversationId, participants);
        String messageJson = JsonConverter.toMessageJson(userId, conversationId, message);
        String metadataJson = JsonConverter.toConversationMetadataJson(userId, conversationId, metadata);
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION, adId, Visibility.ACTIVE.getCode(), MessageNotification.RECEIVE.getCode(),
                participantsJson, messageJson, metadataJson, userId, conversationId));
    }

    private void updateAdConversationIndex(BatchStatement batch, String userId, String conversationId, String adId, UUID messageId) {
        batch.add(cassandraTemplate.bind(Statements.UPDATE_AD_CONVERSATION_INDEX, Visibility.ACTIVE.getCode(), messageId, userId, adId, conversationId));
    }

    private void updateConversationLatestMessage(BatchStatement batch, String userId, String conversationId, Message message) {
        String messageJson = JsonConverter.toMessageJson(userId, conversationId, message);
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_LATEST_MESSAGE, Visibility.ACTIVE.getCode(), messageJson, userId, conversationId));
    }

    private void updateConversationUnreadCount(BatchStatement batch, String userId, String conversationId, String adId) {
        int newUnreadCount = getConversationUnreadCount(userId, conversationId) + 1;
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_UNREAD_COUNT, newUnreadCount, userId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT, newUnreadCount, userId, adId, conversationId));
    }

    private void updateOtherParticipantConversationUnreadCount(BatchStatement batch, String conversationId, String senderUserId) {
        int newOtherParticipantUnreadCount = getConversationOtherParticipantUnreadCount(senderUserId, conversationId) + 1;
        batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT, newOtherParticipantUnreadCount, senderUserId, conversationId));
    }

    private void insertMessage(BatchStatement batch, String userId, String conversationId, Message message) {
        String messageMetadata = JsonConverter.toMessageMetadataJson(userId, conversationId, message);
        batch.add(cassandraTemplate.bind(Statements.INSERT_MESSAGE, userId, conversationId, message.getId(), message.getType().getValue(), messageMetadata));
    }

    private void insertAdConversationModificationIdx(BatchStatement batch, String userId, String conversationId, String adId, UUID messageId) {
        batch.add(cassandraTemplate.bind(Statements.INSERT_AD_CONVERSATION_MODIFICATION_IDX, userId, conversationId, messageId, adId));
    }

    @Override
    public void archiveConversations(String userId, Map<String, String> conversationAdIdsMap) {
        internalConversationsVisibility(userId, conversationAdIdsMap, Visibility.ARCHIVED);
    }

    @Override
    public void activateConversations(String userId, Map<String, String> conversationAdIdsMap) {
        internalConversationsVisibility(userId, conversationAdIdsMap, Visibility.ACTIVE);
    }

    private void internalConversationsVisibility(String userId, Map<String, String> conversationAdIdsMap, Visibility visibility) {
        BatchStatement batch = new BatchStatement();
        conversationAdIdsMap.forEach((conversationId, adId) -> {
            batch.add(cassandraTemplate.bind(Statements.CHANGE_CONVERSATION_VISIBILITY, visibility.getCode(), userId, conversationId));
            batch.add(cassandraTemplate.bind(Statements.CHANGE_CONVERSATION_IDX_VISIBILITY, visibility.getCode(), userId, adId, conversationId));
            batch.add(cassandraTemplate.bind(Statements.UPDATE_CONVERSATION_UNREAD_COUNT, 0, userId, conversationId));
            batch.add(cassandraTemplate.bind(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT, 0, userId, adId, conversationId));
        });
        cassandraTemplate.execute(batch);
    }

    @Override
    public void deleteConversation(String userId, String conversationId, String adId) {
        BatchStatement batch = new BatchStatement();
        batch.add(cassandraTemplate.bind(Statements.DELETE_CONVERSATION, userId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.DELETE_AD_CONVERSATION_INDEX, userId, adId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.DELETE_CONVERSATION_UNREAD_COUNT, userId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.DELETE_AD_CONVERSATION_UNREAD_COUNT, userId, adId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.DELETE_CONVERSATION_MESSAGES, userId, conversationId));
        batch.add(cassandraTemplate.bind(Statements.DELETE_AD_CONVERSATION_MODIFICATION_IDXS, userId, conversationId));
        cassandraTemplate.execute(batch);
    }

    @Override
    public Map<String, String> getConversationAdIdsMap(String userId, List<String> conversationIds) {
        ResultSet resultSet = cassandraTemplate.execute(cassandraTemplate.bind(Statements.SELECT_AD_IDS, userId, conversationIds));
        return StreamUtils.toStream(resultSet)
                .collect(Collectors.toMap(row -> row.getString("convid"), row -> row.getString("adid")));
    }

    @Override
    public ConversationModification getLastConversationModification(String userId, String convId) {
        Row row = cassandraTemplate.execute(cassandraTemplate.bind(Statements.SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX, userId, convId)).one();
        if (row == null) {
            return null;
        }

        String adId = row.getString("adid");
        UUID msgId = row.getUUID("msgid");
        DateTime lastModifiedDate = new DateTime(unixTimestamp(msgId));

        return new ConversationModification(userId, convId, adId, msgId, lastModifiedDate);
    }

    @Override
    public List<String> resolveConversationIdsByUserIdAndAdId(String userId, String adId, int limit) {
        ResultSet resultSet = cassandraTemplate.execute(cassandraTemplate.bind(Statements.SELECT_CONVERSATION_IDS_BY_USER_ID_AND_AD_ID, userId, adId, limit));
        return StreamUtils
                .toStream(resultSet)
                .map(row -> row.getString("convid"))
                .collect(toList());
    }
}