package com.ecg.messagebox.persistence;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagebox.controllers.requests.EmptyConversationRequest;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.persistence.model.ConversationIndex;
import com.ecg.messagebox.persistence.model.PaginatedConversationIds;
import com.ecg.messagebox.persistence.model.UnreadCounts;
import com.ecg.messagebox.util.StreamUtils;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;
import static com.ecg.messagebox.model.Visibility.get;
import static com.ecg.messagebox.util.uuid.UUIDComparator.staticCompare;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

@Component
public class DefaultCassandraPostBoxRepository implements CassandraPostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCassandraPostBoxRepository.class);

    private final Timer getPaginatedConversationIdsTimer = newTimer("cassandra.postBoxRepo.v2.getPaginatedConversationIds");
    private final Timer getPostBoxTimer = newTimer("cassandra.postBoxRepo.v2.getPostBox");
    private final Timer getConversationMessageNotificationTimer = newTimer("cassandra.postBoxRepo.v2.getConversationMessageNotification");
    private final Timer getConversationWithMessagesTimer = newTimer("cassandra.postBoxRepo.v2.getConversationWithMessages");
    private final Timer getConversationMessagesTimer = newTimer("cassandra.postBoxRepo.v2.getConversationMessages");
    private final Timer createConversationTimer = newTimer("cassandra.postBoxRepo.v2.createConversation");
    private final Timer addMessageTimer = newTimer("cassandra.postBoxRepo.v2.addMessage");
    private final Timer addSystemMessageTimer = newTimer("cassandra.postBoxRepo.v2.addSystemMessage");
    private final Timer getConversationAdIdsMapTimer = newTimer("cassandra.postBoxRepo.v2.getConversationAdIdsMap");
    private final Timer resetConversationUnreadCountTimer = newTimer("cassandra.postBoxRepo.v2.resetConversationUnreadCount");
    private final Timer resetConversationsUnreadCountTimer = newTimer("cassandra.postBoxRepo.v2.resetConversationsUnreadCount");
    private final Timer changeConversationVisibilitiesTimer = newTimer("cassandra.postBoxRepo.v2.changeConversationVisibilities");
    private final Timer getUserUnreadCountsTimer = newTimer("cassandra.postBoxRepo.v2.getUserUnreadCounts");
    private final Timer getConversationUnreadCountMapTimer = newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCountMap");
    private final Timer getConversationUnreadCountTimer = newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCount");
    private final Timer getLastConversationModificationTimer = newTimer("cassandra.postBoxRepo.v2.getLastConversationModificationTimer");
    private final Timer resolveConversationIdsByUserIdAndAdId = newTimer("cassandra.postBoxRepo.v2.resolveConversationIdsByUserIdAndAdId");
    private final Timer createEmptyConversationTimer = newTimer("cassandra.postBoxRepo.v2.createEmptyConversationProjection");

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private Map<Statements, PreparedStatement> preparedStatements;

    @Autowired
    public DefaultCassandraPostBoxRepository(
            @Qualifier("cassandraSessionForMb") Session session,
            @Qualifier("cassandraReadConsistency") ConsistencyLevel readConsistency,
            @Qualifier("cassandraWriteConsistency") ConsistencyLevel writeConsistency) {

        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;
        this.preparedStatements = Statements.prepare(session);
    }

    @Override
    public PostBox getPostBox(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = getPostBoxTimer.time()) {
            PaginatedConversationIds paginatedConversationIds = getPaginatedConversationIds(userId, visibility, conversationsOffset, conversationsLimit);
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS.bind(this, userId, paginatedConversationIds.getConversationIds()));

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
                    }).sorted((c1, c2) -> staticCompare(c2.getLatestMessage().getId(), c1.getLatestMessage().getId()))
                    .collect(toList());

            UserUnreadCounts userUnreadCounts = createUserUnreadCounts(userId, conversationUnreadCountsMap.values().stream()
                    .map(UnreadCounts::getUserUnreadCounts).collect(toList()));

            return new PostBox(userId, conversations, userUnreadCounts, paginatedConversationIds.getConversationsTotalCount());
        }
    }

    public PaginatedConversationIds getPaginatedConversationIds(String userId, Visibility visibility, int offset, int limit) {
        try (Timer.Context ignored = getPaginatedConversationIdsTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_INDICES.bind(this, userId));

            List<String> allConversationIds = StreamUtils.toStream(resultSet)
                    .map(row -> new ConversationIndex(row.getString("convid"), row.getString("adid"), get(row.getInt("vis")), row.getUUID("latestmsgid")))
                    .filter(ci -> ci.getVisibility() == visibility)
                    .sorted((ci1, ci2) -> staticCompare(ci2.getLatestMessageId(), ci1.getLatestMessageId()))
                    .map(ConversationIndex::getConversationId)
                    .collect(toList());

            List<String> paginatedConversationIds = allConversationIds.stream()
                    .skip(offset * limit)
                    .limit(limit)
                    .collect(toList());

            return new PaginatedConversationIds(paginatedConversationIds, allConversationIds.size());
        }
    }

    private Map<String, UnreadCounts> getConversationUnreadCountMap(String userId) {
        try (Timer.Context ignored = getConversationUnreadCountMapTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS_UNREAD_COUNTS.bind(this, userId));
            return StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(
                            row -> row.getString("convid"),
                            row -> new UnreadCounts(row.getInt("unread"), row.getInt("unreadother")))
                    );
        }
    }

    @Override
    public Optional<MessageNotification> getConversationMessageNotification(String userId, String conversationId) {
        try (Timer.Context ignored = getConversationMessageNotificationTimer.time()) {
            Row row = session.execute(Statements.SELECT_CONVERSATION_MESSAGE_NOTIFICATION.bind(this, userId, conversationId)).one();
            return row != null ? of(MessageNotification.get(row.getInt("ntfynew"))) : empty();
        }
    }

    @Override
    public Optional<ConversationThread> getConversationWithMessages(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        LOG.trace("Retrieving conversationThread for conversationId {}, userId {}", conversationId, userId);
        try (Timer.Context ignored = getConversationWithMessagesTimer.time()) {
            Row row = session.execute(Statements.SELECT_CONVERSATION.bind(this, userId, conversationId)).one();
            if (row != null) {
                ConversationThread conversation = createConversation(userId, row);

                for (Participant participant : conversation.getParticipants()) {
                    int unreadMessagesCount = getConversationUnreadCount(participant.getUserId(), conversationId);
                    conversation.addNumUnreadMessages(participant.getUserId(), unreadMessagesCount);
                }

                List<Message> messages = getConversationMessages(userId, conversationId, messageIdCursorOpt, messagesLimit);
                conversation.addMessages(messages);

                return of(conversation);
            } else {
                LOG.trace("Could not get conversationThread for conversationId {}, userId {}", conversationId, userId);
                return empty();
            }
        }
    }

    public List<Message> getConversationMessages(String userId, String conversationId, Optional<String> cursorOpt, int limit) {
        if (limit < 1) {
            return Collections.emptyList();
        }
        try (Timer.Context ignored = getConversationMessagesTimer.time()) {
            ResultSet resultSet = cursorOpt
                    .map(cursor -> session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITH_CURSOR.bind(this, userId, conversationId, UUID.fromString(cursor), limit)))
                    .orElse(session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR.bind(this, userId, conversationId, limit)));

            return StreamUtils.toStream(resultSet)
                    .map(row -> {
                        UUID messageId = row.getUUID("msgid");
                        MessageMetadata metadata = JsonConverter.fromMessageMetadataJson(userId, conversationId, messageId.toString(), row.getString("metadata"));
                        return new Message(row.getUUID("msgid"),
                                MessageType.get(row.getString("type")),
                                metadata);
                    })
                    .sorted((m1, m2) -> staticCompare(m1.getId(), m2.getId()))
                    .collect(toList());
        }
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
        try (Timer.Context ignored = getUserUnreadCountsTimer.time()) {
            ResultSet result = session.execute(Statements.SELECT_USER_UNREAD_COUNTS.bind(this, userId));
            List<Integer> conversationUnreadCounts = StreamUtils.toStream(result)
                    .map(row -> row.getInt("unread"))
                    .collect(toList());
            return createUserUnreadCounts(userId, conversationUnreadCounts);
        }
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
        try (Timer.Context ignored = getConversationUnreadCountTimer.time()) {
            Row unreadCountResult = session.execute(Statements.SELECT_CONVERSATION_UNREAD_COUNT.bind(this, userId, conversationId)).one();
            return unreadCountResult == null ? 0 : unreadCountResult.getInt("unread");
        }
    }

    @Override
    public int getConversationOtherParticipantUnreadCount(String userId, String conversationId) {
        try (Timer.Context ignored = getConversationUnreadCountTimer.time()) {
            Row unreadCountResult = session.execute(Statements.SELECT_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT.bind(this, userId, conversationId)).one();
            return unreadCountResult == null ? 0 : unreadCountResult.getInt("unreadother");
        }
    }

    @Override
    public void resetConversationUnreadCount(String userId, String otherParticipantUserId, String conversationId, String adId) {
        try (Timer.Context ignored = resetConversationUnreadCountTimer.time()) {
            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, conversationId));
            batch.add(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT.bind(this, 0, otherParticipantUserId, conversationId));
            batch.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, adId, conversationId));

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void resetConversationsUnreadCount(PostBox postBox) {
        try (Timer.Context ignored = resetConversationsUnreadCountTimer.time()) {
            BatchStatement batch = new BatchStatement();

            for (ConversationThread conversation : postBox.getConversations()) {
                if (conversation.getNumUnreadMessages(postBox.getUserId()) > 0) {
                    batch.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, postBox.getUserId(), conversation.getId()));
                    batch.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, 0, postBox.getUserId(), conversation.getAdId(), conversation.getId()));

                    List<Participant> participants = conversation.getParticipants();
                    String otherParticipantUserId = participants.get(0).getUserId().equals(postBox.getUserId()) ? participants.get(1).getUserId() : participants.get(0).getUserId();
                    batch.add(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT.bind(this, 0, otherParticipantUserId, conversation.getId()));
                }
            }

            if (!batch.getStatements().isEmpty()) {
                batch.setConsistencyLevel(getWriteConsistency());
                session.execute(batch);
            }
        }
    }

    @Override
    public void createConversation(String userId, ConversationThread conversation, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = createConversationTimer.time()) {
            createConversation(conversation.getId(), userId, conversation.getAdId(), conversation.getParticipants(), message, conversation.getMetadata(), incrementUnreadCount);
        }
    }

    @Override
    public void addMessage(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = addMessageTimer.time()) {
            String messageJson = JsonConverter.toMessageJson(userId, conversationId, message);

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_LATEST_MESSAGE.bind(this, Visibility.ACTIVE.getCode(), messageJson, userId, conversationId));
            newMessageCqlStatements(userId, conversationId, adId, message, incrementUnreadCount).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void addSystemMessage(String userId, String conversationId, String adId, Message message) {
        try (Timer.Context ignored = addSystemMessageTimer.time()) {
            String messageJson = JsonConverter.toMessageJson(userId, conversationId, message);

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_LATEST_MESSAGE.bind(this, Visibility.ACTIVE.getCode(), messageJson, userId, conversationId));
            newSystemMessageCqlStatements(userId, conversationId, adId, message).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    private List<Statement> newMessageCqlStatements(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        return newMessageCqlStatements(userId, conversationId, adId, message, incrementUnreadCount, true);
    }

    private List<Statement> newMessageCqlStatements(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount, boolean insertMessage) {
        List<Statement> statements = new ArrayList<>();
        statements.add(Statements.UPDATE_AD_CONVERSATION_INDEX.bind(this, Visibility.ACTIVE.getCode(), message.getId(), userId, adId, conversationId));

        if (insertMessage) {
            String messageMetadata = JsonConverter.toMessageMetadataJson(userId, conversationId, message);
            statements.add(Statements.INSERT_MESSAGE.bind(this, userId, conversationId, message.getId(), message.getType().getValue(), messageMetadata));
        }

        if (incrementUnreadCount) {
            int newUnreadCount = getConversationUnreadCount(userId, conversationId) + 1;
            int newOtherParticipantUnreadCount = getConversationOtherParticipantUnreadCount(message.getSenderUserId(), conversationId) + 1;
            statements.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, conversationId));
            statements.add(Statements.UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT.bind(this, newOtherParticipantUnreadCount, message.getSenderUserId(), conversationId));
            statements.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, adId, conversationId));
        }

        statements.add(Statements.INSERT_AD_CONVERSATION_MODIFICATION_IDX.bind(this, userId, conversationId, message.getId(), adId));

        return statements;
    }

    private List<Statement> newSystemMessageCqlStatements(String userId, String conversationId, String adId, Message message) {
        List<Statement> statements = new ArrayList<>();
        statements.add(Statements.UPDATE_AD_CONVERSATION_INDEX.bind(this, Visibility.ACTIVE.getCode(), message.getId(), userId, adId, conversationId));
        String messageMetadata = JsonConverter.toMessageMetadataJson(userId, conversationId, message);
        statements.add(Statements.INSERT_MESSAGE.bind(this, userId, conversationId, message.getId(), message.getType().getValue(), messageMetadata));

        int newUnreadCount = getConversationUnreadCount(userId, conversationId) + 1;
        statements.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, conversationId));
        statements.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, adId, conversationId));

        statements.add(Statements.INSERT_AD_CONVERSATION_MODIFICATION_IDX.bind(this, userId, conversationId, message.getId(), adId));

        return statements;
    }

    @Override
    public void changeConversationVisibilities(String userId, Map<String, String> conversationAdIdsMap, Visibility visibility) {
        try (Timer.Context ignored = changeConversationVisibilitiesTimer.time()) {
            BatchStatement batch = new BatchStatement();
            conversationAdIdsMap.forEach((conversationId, adId) -> {
                batch.add(Statements.CHANGE_CONVERSATION_VISIBILITY.bind(this, visibility.getCode(), userId, conversationId));
                batch.add(Statements.CHANGE_CONVERSATION_IDX_VISIBILITY.bind(this, visibility.getCode(), userId, adId, conversationId));
                batch.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, conversationId));
                batch.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, adId, conversationId));
            });
            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void deleteConversation(String userId, String conversationId, String adId) {
        BatchStatement batch = new BatchStatement();
        batch.add(Statements.DELETE_CONVERSATION.bind(this, userId, conversationId));
        batch.add(Statements.DELETE_AD_CONVERSATION_INDEX.bind(this, userId, adId, conversationId));
        batch.add(Statements.DELETE_CONVERSATION_UNREAD_COUNT.bind(this, userId, conversationId));
        batch.add(Statements.DELETE_AD_CONVERSATION_UNREAD_COUNT.bind(this, userId, adId, conversationId));
        batch.add(Statements.DELETE_CONVERSATION_MESSAGES.bind(this, userId, conversationId));
        batch.add(Statements.DELETE_AD_CONVERSATION_MODIFICATION_IDXS.bind(this, userId, conversationId));
        batch.setConsistencyLevel(getWriteConsistency());
        session.execute(batch);
    }

    @Override
    public Map<String, String> getConversationAdIdsMap(String userId, List<String> conversationIds) {
        try (Timer.Context ignored = getConversationAdIdsMapTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_AD_IDS.bind(this, userId, conversationIds));
            return StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(row -> row.getString("convid"), row -> row.getString("adid")));
        }
    }

    @Override
    public ConversationModification getLastConversationModification(String userId, String convId) {
        try (Timer.Context ignored = getLastConversationModificationTimer.time()) {
            Row row = session.execute(Statements.SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX.bind(this, userId, convId)).one();
            if (row == null) {
                return null;
            }

            String adId = row.getString("adid");
            UUID msgId = row.getUUID("msgid");
            DateTime lastModifiedDate = new DateTime(unixTimestamp(msgId));

            return new ConversationModification(userId, convId, adId, msgId, lastModifiedDate);
        }
    }

    public List<String> resolveConversationIdsByUserIdAndAdId(String userId, String adId, int limit) {
        try (Timer.Context ignored = resolveConversationIdsByUserIdAndAdId.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_IDS_BY_USER_ID_AND_AD_ID.bind(this, userId, adId, limit));
            return StreamUtils
                    .toStream(resultSet)
                    .map(row -> row.getString("convid"))
                    .collect(toList());
        }
    }

    @Override
    public String createEmptyConversationProjection(EmptyConversationRequest emptyConversation, String newConversationId, String userId) {

        String imageUrl = emptyConversation.getCustomValues().get("imageUrl");
        try (Timer.Context ignored = createEmptyConversationTimer.time()) {

            createConversation(
                    newConversationId,
                    userId,
                    emptyConversation.getAdId(),
                    new ArrayList<>(emptyConversation.getParticipants().values()),
                    emptyConversation.getMessage(),
                    new ConversationMetadata(DateTime.now(), emptyConversation.getEmailSubject(), emptyConversation.getAdTitle(), imageUrl),
                    false,
                    false
            );

            return newConversationId;
        }
    }

    private void createConversation(String conversationId, String senderId, String adId, List<Participant> participants, Message message, ConversationMetadata metadata, boolean incrementUnreadCount) {
        createConversation(conversationId, senderId, adId, participants, message, metadata, incrementUnreadCount, true);
    }

    private void createConversation(String conversationId, String senderId, String adId, List<Participant> participants, Message message, ConversationMetadata metadata, boolean incrementUnreadCount, boolean insertMessage) {

        String participantsJson = JsonConverter.toParticipantsJson(senderId, conversationId, participants);
        String messageJson = JsonConverter.toMessageJson(senderId, conversationId, message);
        String metadataJson = JsonConverter.toConversationMetadataJson(senderId, conversationId, metadata);

        BatchStatement batch = new BatchStatement();

        batch.add(Statements.UPDATE_CONVERSATION.bind(this, adId, Visibility.ACTIVE.getCode(), MessageNotification.RECEIVE.getCode(),
                participantsJson, messageJson, metadataJson, senderId, conversationId));
        newMessageCqlStatements(senderId, conversationId, adId, message, incrementUnreadCount, insertMessage).forEach(batch::add);

        batch.setConsistencyLevel(getWriteConsistency());
        session.execute(batch);
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    enum Statements {
        // select the user's unread counts
        SELECT_USER_UNREAD_COUNTS("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ?"),

        // select the ad's unread counts
        SELECT_AD_UNREAD_COUNT("SELECT unread FROM mb_ad_conversation_unread_counts WHERE usrid = ? and adid = ?"),

        // select the user's conversations
        SELECT_CONVERSATION_INDICES("SELECT convid, adid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrid = ?"),
        SELECT_AD_CONVERSATION_INDICES("SELECT convid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrId = ? AND adid = ?"),
        SELECT_CONVERSATIONS("SELECT convid, vis, ntfynew, participants, adid, latestmsg, metadata FROM mb_conversations WHERE usrid = ? AND convid IN ?"),
        SELECT_CONVERSATIONS_UNREAD_COUNTS("SELECT convid, unread, unreadother FROM mb_conversation_unread_counts WHERE usrid = ?"),

        SELECT_AD_IDS("SELECT convid, adid FROM mb_conversations WHERE usrid = ? AND convid IN ?"),

        // select single conversation + messages
        SELECT_CONVERSATION("SELECT convid, adid, vis, ntfynew, participants, latestmsg, metadata FROM mb_conversations WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_MESSAGE_NOTIFICATION("SELECT ntfynew FROM mb_conversations WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_UNREAD_COUNT("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT("SELECT unreadother FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? LIMIT ?"),
        SELECT_CONVERSATION_MESSAGES_WITH_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? AND msgid < ? LIMIT ?"),
        SELECT_CONVERSATION_IDS_BY_USER_ID_AND_AD_ID("SELECT convid from mb_ad_conversation_idx WHERE usrid = ? AND adid = ? LIMIT ?"),
        SELECT_CONVERSATION_AD_ID("SELECT adid FROM mb_conversations WHERE usrid = ? AND convid = ?"),

        // update a single conversation when a new message comes in
        UPDATE_CONVERSATION_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unread = ? WHERE usrid = ? AND convid = ?", true),
        UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unreadother = ? WHERE usrid = ? AND convid = ?", true),
        UPDATE_AD_CONVERSATION_UNREAD_COUNT("UPDATE mb_ad_conversation_unread_counts SET unread = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

        UPDATE_AD_CONVERSATION_INDEX("UPDATE mb_ad_conversation_idx SET vis = ?, latestmsgid = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),
        UPDATE_CONVERSATION_LATEST_MESSAGE("UPDATE mb_conversations SET vis = ?, latestmsg = ? WHERE usrid = ? AND convid = ?", true),
        UPDATE_CONVERSATION("UPDATE mb_conversations SET adid = ?, vis = ?, ntfynew = ?, participants = ?, latestmsg = ?, metadata = ? WHERE usrid = ? AND convid = ?", true),
        INSERT_MESSAGE("INSERT INTO mb_messages (usrid, convid, msgid, type, metadata) VALUES (?, ?, ?, ?, ?)", true),

        CHANGE_CONVERSATION_VISIBILITY("UPDATE mb_conversations SET vis = ? WHERE usrid = ? AND convid = ?", true),
        CHANGE_CONVERSATION_IDX_VISIBILITY("UPDATE mb_ad_conversation_idx SET vis = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

        // cleanup of old messages and conversations
        INSERT_AD_CONVERSATION_MODIFICATION_IDX("INSERT INTO mb_ad_conversation_modification_idx (usrid, convid, msgid, adid) VALUES (?, ?, ?, ?)", true),

        SELECT_AD_CONVERSATION_MODIFICATION_IDXS("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?"),
        SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? LIMIT 1"),

        DELETE_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_ad_conversation_unread_counts WHERE usrid = ? AND adid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_INDEX("DELETE FROM mb_ad_conversation_idx WHERE usrid = ? AND adid = ? AND convid = ?", true),
        DELETE_CONVERSATION("DELETE FROM mb_conversations WHERE usrid = ? AND convid = ?", true),
        DELETE_CONVERSATION_MESSAGES("DELETE FROM mb_messages WHERE usrid = ? AND convid = ?", true),

        DELETE_AD_CONVERSATION_MODIFICATION_IDX("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? AND msgid = ?", true),
        DELETE_AD_CONVERSATION_MODIFICATION_IDXS("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?", true);

        private final String cql;
        private final boolean modifying;

        Statements(String cql) {
            this(cql, false);
        }

        Statements(String cql, boolean modifying) {
            this.cql = cql;
            this.modifying = modifying;
        }

        public static Map<Statements, PreparedStatement> prepare(Session session) {
            Map<Statements, PreparedStatement> statements = new EnumMap<>(Statements.class);
            for (Statements statement : values()) {
                statements.put(statement, session.prepare(statement.cql));
            }
            return ImmutableMap.copyOf(statements);
        }

        public Statement bind(DefaultCassandraPostBoxRepository repository, Object... values) {
            return repository.preparedStatements
                    .get(this)
                    .bind(values)
                    .setConsistencyLevel(getConsistencyLevel(repository))
                    .setIdempotent(!modifying);
        }

        private ConsistencyLevel getConsistencyLevel(DefaultCassandraPostBoxRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}