package com.ecg.messagebox.persistence.cassandra;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.persistence.cassandra.model.ConversationIndex;
import com.ecg.messagebox.utils.StreamUtils;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;
import static com.ecg.messagebox.model.Visibility.get;
import static com.ecg.messagecenter.util.MessageCenterUtils.truncateText;
import static com.google.common.collect.Lists.newArrayList;

public class DefaultCassandraPostBoxRepository implements CassandraPostBoxRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCassandraPostBoxRepository.class);

    private static final int TIMEOUT_IN_SECONDS = 5;

    private final String keyspace;
    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private final UserType messageUserType;
    private final UserType participantUserType;

    private final int messageTextPreviewMaxChars;

    private Map<Statements, PreparedStatement> preparedStatements;

    private final ExecutorService executorService;

    private final Timer getPaginatedConversationIdsTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getPaginatedConversationIds");
    private final Timer getPostBoxTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getPostBox");
    private final Timer getConversationTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversation");
    private final Timer getConversationWithMessagesTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversationWithMessages");
    private final Timer getConversationIdsTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversationIds");
    private final Timer getConversationMessagesTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversationMessages");
    private final Timer createConversationTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.createConversation");
    private final Timer addMessageTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.addMessage");

    private final Timer getAdConversationIdsMapTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getAdConversationIdsMap");

    private final Timer resetConversationUnreadCountTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.resetConversationUnreadCount");

    private final Timer changeConversationVisibilitiesTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.changeConversationVisibilities");

    private final Timer getPostBoxUnreadCountsTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getPostBoxUnreadCounts");
    private final Timer getConversationUnreadCountMapTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCountMap");
    private final Timer getConversationUnreadCountTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCount");

    private final Timer blockUserTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.blockUser");
    private final Timer unblockUserTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.unblockUser");
    private final Timer getBlockedUserInfoTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.getBlockedUserInfo");

    private final Timer deleteConversationsTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.deleteConversations");
    private final Timer deleteConversationTimer = TimingReports.newTimer("cassandra.postBoxRepo.v2.deleteConversation");

    public DefaultCassandraPostBoxRepository(String keyspace, Session session,
                                             ConsistencyLevel readConsistency, ConsistencyLevel writeConsistency,
                                             int messageTextPreviewMaxChars,
                                             boolean writeToNewDataModel) {
        this.keyspace = keyspace;
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;

        this.messageTextPreviewMaxChars = messageTextPreviewMaxChars;

        this.messageUserType = getUserType("conversation_message");
        this.participantUserType = getUserType("conversation_participant");

        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        if (writeToNewDataModel) {
            preparedStatements = Statements.prepare(session);
        }
    }

    @Override
    public PostBox getPostBox(String postBoxId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = getPostBoxTimer.time()) {

            Future<List<String>> conversationIdsFuture =
                    executorService.submit(() -> getPaginatedConversationIds(postBoxId, visibility, conversationsOffset, conversationsLimit));
            Future<Map<String, Integer>> unreadCountsFuture =
                    executorService.submit(() -> getConversationUnreadCountMap(postBoxId));

            try {
                List<String> conversationIds = conversationIdsFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS.bind(this, postBoxId, conversationIds));

                Map<String, Integer> conversationUnreadCountsMap = unreadCountsFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

                List<ConversationThread> conversations = StreamUtils.toStream(resultSet)
                        .map(row -> {
                            ConversationThread conversation = createConversation(row);
                            conversation.addNumUnreadMessages(conversationUnreadCountsMap.getOrDefault(row.getString("convid"), 0));
                            return conversation;
                        })
                        .collect(Collectors.toList());

                PostBoxUnreadCounts postBoxUnreadCounts = createPostBoxUnreadCounts(postBoxId, conversationUnreadCountsMap.values());

                return new PostBox(postBoxId, conversations, postBoxUnreadCounts);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                conversationIdsFuture.cancel(true);
                unreadCountsFuture.cancel(true);
                LOGGER.error("Could not fetch conversation indices for postbox id {} and visibility {}", postBoxId, visibility.name(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> getPaginatedConversationIds(String postBoxId, Visibility visibility, int offset, int limit) {
        try (Timer.Context ignored = getPaginatedConversationIdsTimer.time()) {

            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_INDICES.bind(this, postBoxId));

            return StreamUtils.toStream(resultSet)
                    .map(row -> new ConversationIndex(row.getString("convid"),
                            row.getString("adid"),
                            get(row.getInt("visibility")),
                            row.getUUID("latestmsgid")))
                    .filter(ci -> ci.getVisibility() == visibility)
                    .sorted((ci1, ci2) -> ci2.getLatestMessageId().compareTo(ci1.getLatestMessageId()))
                    .skip(offset * limit)
                    .limit(limit)
                    .map(ConversationIndex::getConversationId)
                    .collect(Collectors.toList());
        }
    }

    private Map<String, Integer> getConversationUnreadCountMap(String postBoxId) {
        try (Timer.Context ignored = getConversationUnreadCountMapTimer.time()) {

            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS_UNREAD_COUNTS.bind(this, postBoxId));
            return StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(
                            row -> row.getString("convid"),
                            row -> row.getInt("unread")));
        }
    }

    @Override
    public Optional<ConversationThread> getConversation(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            Row row = session.execute(Statements.SELECT_CONVERSATION.bind(this, postBoxId, conversationId)).one();
            return row != null ? Optional.of(createConversation(row)) : Optional.empty();
        }
    }

    @Override
    public Optional<ConversationThread> getConversationWithMessages(String postBoxId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = getConversationWithMessagesTimer.time()) {

            Future<Integer> unreadMessagesCountFuture = executorService.submit(() -> getConversationUnreadCount(postBoxId, conversationId));
            Future<Row> conversationFuture = executorService.submit(() -> session.execute(Statements.SELECT_CONVERSATION.bind(this, postBoxId, conversationId)).one());
            Future<List<Message>> messagesFuture = executorService.submit(() -> getConversationMessages(postBoxId, conversationId, messageIdCursorOpt, messagesLimit));

            try {
                Row row = conversationFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                if (row != null) {
                    ConversationThread conversation = createConversation(row);

                    int unreadMessagesCount = unreadMessagesCountFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                    conversation.addNumUnreadMessages(unreadMessagesCount);

                    List<Message> messages = messagesFuture.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                    conversation.addMessages(messages);

                    return Optional.of(conversation);
                } else {
                    return Optional.empty();
                }

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                unreadMessagesCountFuture.cancel(true);
                conversationFuture.cancel(true);
                messagesFuture.cancel(true);
                LOGGER.error("Could not fetch conversation for user identifier {} and conversation id {}", postBoxId, conversationId, e);
                throw new RuntimeException(e);
            }
        }
    }

    private List<Message> getConversationMessages(String postBoxId, String conversationId, Optional<String> cursorOpt, int limit) {
        try (Timer.Context ignored = getConversationMessagesTimer.time()) {

            ResultSet resultSet = cursorOpt
                    .map(cursor -> session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITH_CURSOR.bind(this, postBoxId, conversationId, UUID.fromString(cursor), limit)))
                    .orElse(session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR.bind(this, postBoxId, conversationId, limit)));

            return StreamUtils.toStream(resultSet)
                    .map(row -> new Message(row.getUUID("msgid"),
                            row.getString("msg"),
                            row.getString("senderid"),
                            MessageType.get(row.getString("type"))))
                    .collect(Collectors.toList());
        }
    }

    private ConversationThread createConversation(Row row) {
        String conversationId = row.getString("convid");
        String adId = row.getString("adid");

        Visibility visibility = get(row.getInt("visibility"));
        MessageNotification messageNotification = MessageNotification.get(row.getInt("notifynew"));

        UDTValue meParticipantUDTValue = row.getUDTValue("meparticipant");
        Participant meParticipant = new Participant(meParticipantUDTValue.getString("participantid"),
                meParticipantUDTValue.getString("name"),
                meParticipantUDTValue.getString("email"),
                ParticipantRole.get(meParticipantUDTValue.getString("role")));

        UDTValue otherParticipantUDTValue = row.getUDTValue("otherparticipant");
        Participant otherParticipant = new Participant(otherParticipantUDTValue.getString("participantid"),
                otherParticipantUDTValue.getString("name"),
                otherParticipantUDTValue.getString("email"),
                ParticipantRole.get(otherParticipantUDTValue.getString("role")));

        UDTValue latestMessageUDTValue = row.getUDTValue("latestmsgprev");
        Message latestMessage = new Message(latestMessageUDTValue.getUUID("msgid"),
                latestMessageUDTValue.getString("msg"),
                latestMessageUDTValue.getString("senderid"),
                MessageType.get(latestMessageUDTValue.getString("type")));

        return new ConversationThread(conversationId, adId, visibility, messageNotification, meParticipant, otherParticipant, latestMessage);
    }

    @Override
    public PostBoxUnreadCounts getPostBoxUnreadCounts(String postBoxId) {
        try (Timer.Context ignored = getPostBoxUnreadCountsTimer.time()) {

            ResultSet result = session.execute(Statements.SELECT_POSTBOX_UNREAD_COUNTS.bind(this, postBoxId));
            List<Integer> conversationUnreadCounts = StreamUtils.toStream(result)
                    .map(row -> row.getInt("unread"))
                    .collect(Collectors.toList());
            return createPostBoxUnreadCounts(postBoxId, conversationUnreadCounts);
        }
    }

    private PostBoxUnreadCounts createPostBoxUnreadCounts(String postBoxId, Collection<Integer> conversationUnreadCounts) {
        CountersConsumer counters = conversationUnreadCounts.stream()
                .collect(CountersConsumer::new, CountersConsumer::add, CountersConsumer::reduce);
        return new PostBoxUnreadCounts(postBoxId, counters.numUnreadConversations, counters.numUnreadMessages);
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
    public int getConversationUnreadCount(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationUnreadCountTimer.time()) {

            Row unreadCountResult = session.execute(Statements.SELECT_CONVERSATION_UNREAD_COUNT.bind(this, postBoxId, conversationId)).one();
            return unreadCountResult == null ? 0 : unreadCountResult.getInt("unread");
        }
    }

    @Override
    public void resetConversationUnreadCount(String postBoxId, String conversationId, String adId) {
        try (Timer.Context ignored = resetConversationUnreadCountTimer.time()) {

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, postBoxId, conversationId));
            batch.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, 0, postBoxId, adId, conversationId));

//            batch.add(Statements.UPDATE_CONVERSATION_UNREAD_MSG_COUNT.bind(this, 0, postBoxId, conversationId));
//            batch.add(Statements.UPDATE_AD_CONVERSATION_IDX_UNREAD_MSG_COUNT.bind(this, 0, postBoxId, adId, conversationId));

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void createConversation(String postBoxId, ConversationThread conversation, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = createConversationTimer.time()) {

            BatchStatement batch = new BatchStatement();

            Participant meParticipant = conversation.getMeParticipant();
            UDTValue meParticipantUDTValue = participantUserType.newValue()
                    .setString("participantid", meParticipant.getUserId())
                    .setString("name", meParticipant.getName())
                    .setString("email", meParticipant.getEmail())
                    .setString("role", meParticipant.getRole().getValue());

            Participant otherParticipant = conversation.getOtherParticipant();
            UDTValue otherParticipantUDTValue = participantUserType.newValue()
                    .setString("participantid", otherParticipant.getUserId())
                    .setString("name", otherParticipant.getName())
                    .setString("email", otherParticipant.getEmail())
                    .setString("role", otherParticipant.getRole().getValue());

            batch.add(Statements.UPDATE_CONVERSATION.bind(this, conversation.getAdId(), Visibility.ACTIVE.getCode(), MessageNotification.RECEIVE.getCode(),
                    meParticipantUDTValue, otherParticipantUDTValue, messagePreviewUDTValue(message), postBoxId, conversation.getId()));

            newMessageCqlStatements(postBoxId, conversation.getId(), conversation.getAdId(), message, incrementUnreadCount).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void addMessage(String postBoxId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = addMessageTimer.time()) {

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_LATEST_MESSAGE.bind(this, Visibility.ACTIVE.getCode(), messagePreviewUDTValue(message), postBoxId, conversationId));
            newMessageCqlStatements(postBoxId, conversationId, adId, message, incrementUnreadCount).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    private UDTValue messagePreviewUDTValue(Message message) {
        String messageTypeValue = message.getType().getValue();
        return messageUserType.newValue()
                .setUUID("msgid", message.getId())
                .setString("msg", truncateText(message.getText(), messageTextPreviewMaxChars))
                .setString("senderid", message.getSenderUserId())
                .setString("type", messageTypeValue);
    }

    private List<Statement> newMessageCqlStatements(String postBoxId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        List<Statement> statements = new ArrayList<>();

        statements.add(Statements.UPDATE_AD_CONVERSATION_INDEX.bind(this, Visibility.ACTIVE.getCode(), message.getId(), postBoxId, adId, conversationId));

        statements.add(Statements.INSERT_MESSAGE.bind(this, postBoxId, conversationId,
                message.getId(), message.getText(), message.getSenderUserId(), message.getType().getValue()));

        if (incrementUnreadCount) {
            int newUnreadCount = getConversationUnreadCount(postBoxId, conversationId) + 1;
            statements.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, postBoxId, conversationId));
            statements.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, postBoxId, adId, conversationId));
        }

        statements.add(Statements.INSERT_AD_CONVERSATION_MODIFICATION_IDX.bind(this, postBoxId, conversationId, message.getId(), adId));
        Date modifiedDate = new DateTime(unixTimestamp(message.getId())).hourOfDay().roundFloorCopy().toDate();
        statements.add(Statements.INSERT_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, modifiedDate, message.getId(), postBoxId, conversationId));

        return statements;
    }

    @Override
    public void changeConversationVisibilities(String postBoxId, Map<String, String> adConversationIdsMap, Visibility visibility) {
        try (Timer.Context ignored = changeConversationVisibilitiesTimer.time()) {

            BatchStatement batch = new BatchStatement();
            adConversationIdsMap.forEach((adId, conversationId) -> {
                batch.add(Statements.CHANGE_CONVERSATION_VISIBILITY.bind(this, visibility.getCode(), postBoxId, conversationId));
                batch.add(Statements.CHANGE_CONVERSATION_IDX_VISIBILITY.bind(this, visibility.getCode(), postBoxId, adId, conversationId));
            });
            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void deleteConversations(String postBoxId, Map<String, String> adConversationIdsMap) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            adConversationIdsMap.forEach((adId, conversationId) -> {
                BatchStatement batch = new BatchStatement();
                batch.add(Statements.DELETE_AD_CONVERSATION.bind(this, postBoxId, conversationId));
                batch.add(Statements.DELETE_AD_CONVERSATION_INDEX.bind(this, postBoxId, adId, conversationId));
                batch.add(Statements.DELETE_CONVERSATION_UNREAD_COUNT.bind(this, postBoxId, conversationId));
                batch.add(Statements.DELETE_AD_CONVERSATION_UNREAD_COUNT.bind(this, postBoxId, adId, conversationId));
                batch.add(Statements.DELETE_CONVERSATION_MESSAGES.bind(this, postBoxId, conversationId));
                batch.setConsistencyLevel(getWriteConsistency());
                session.execute(batch);

                ResultSet resultSet = session.execute(Statements.SELECT_AD_CONVERSATION_MODIFICATION_IDXS.bind(this, postBoxId, conversationId));
                StreamUtils.toStream(resultSet)
                        .map(row -> row.getUUID("msgid"))
                        .forEach(messageId -> {
                            Date modifiedDate = new DateTime(unixTimestamp(messageId)).hourOfDay().roundFloorCopy().toDate();
                            session.execute(Statements.DELETE_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, modifiedDate, messageId, postBoxId, conversationId));
                        });
                session.execute(Statements.DELETE_AD_CONVERSATION_MODIFICATION_IDXS.bind(this, postBoxId, conversationId));
            });
        }
    }

    @Override
    public void deleteConversation(String postBoxId, String adId, String conversationId) {
        try (Timer.Context ignored = deleteConversationTimer.time()) {
            deleteConversations(postBoxId, Collections.singletonMap(adId, conversationId));
        }
    }

    @Override
    public Map<String, String> getAdConversationIdsMap(String postBoxId, List<String> conversationIds) {
        try (Timer.Context ignored = getAdConversationIdsMapTimer.time()) {

            ResultSet resultSet = session.execute(Statements.SELECT_AD_IDS.bind(this, postBoxId, conversationIds));
            return StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(row -> row.getString("adid"), row -> row.getString("convid")));
        }
    }

    @Override
    public List<String> getConversationIds(String postBoxId) {
        try (Timer.Context ignored = getConversationIdsTimer.time()) {

            ResultSet result = session.execute(Statements.SELECT_CONVERSATION_IDS.bind(this, postBoxId));
            return StreamUtils.toStream(result)
                    .map(row -> row.getString("convid"))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void blockUser(String reporterUserId, String userIdToBlock) {
        try (Timer.Context ignored = blockUserTimer.time()) {
            session.execute(Statements.BLOCK_USER.bind(this, reporterUserId, userIdToBlock));
        }
    }

    @Override
    public void unblockUser(String reporterUserId, String userIdToUnblock) {
        try (Timer.Context ignored = unblockUserTimer.time()) {
            session.execute(Statements.UNBLOCK_USER.bind(this, reporterUserId, userIdToUnblock));
        }
    }

    @Override
    public Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId2) {
        try (Timer.Context ignored = getBlockedUserInfoTimer.time()) {

            List<ListenableFuture<ResultSet>> futures = Futures.inCompletionOrder(
                    newArrayList(
                            session.executeAsync(Statements.SELECT_BLOCKED_USER.bind(this, userId1, userId2)),
                            session.executeAsync(Statements.SELECT_BLOCKED_USER.bind(this, userId2, userId1))));

            for (ListenableFuture<ResultSet> future : futures) {
                try {
                    Row row = future.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).one();
                    if (row != null) {
                        return Optional.of(new BlockedUserInfo(
                                row.getString("blockerid"),
                                row.getString("blockeeid"),
                                new DateTime(row.getDate("blockdate"))));
                    }
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    futures.forEach(f -> f.cancel(true));
                    LOGGER.error("Could not get blocked user information for user id {} and user id {}", userId1, userId2, e);
                    throw new RuntimeException(e);
                }
            }

            return Optional.empty();
        }
    }

    @Override
    public boolean areUsersBlocked(String userId1, String userId2) {
        return getBlockedUserInfo(userId1, userId2).isPresent();
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    private UserType getUserType(String userType) {
        return session.getCluster().getMetadata().getKeyspace(keyspace).getUserType(userType);
    }

    enum Statements {

        // select unread counts for postbox
        SELECT_POSTBOX_UNREAD_COUNTS("SELECT unread FROM mb_conversation_unread_counts WHERE pbid = ?"),

        // select unread counts for ad
        SELECT_AD_UNREAD_COUNT("SELECT unread FROM mb_ad_conversation_unread_counts WHERE pbid = ? and adid = ?"),

        // select postbox
        SELECT_CONVERSATION_INDICES("SELECT convid, adid, visibility, latestmsgid FROM mb_ad_conversation_idx WHERE pbid = ?"),
        SELECT_CONVERSATION_IDS("SELECT convid FROM mb_ad_conversation_idx WHERE pbid = ?"),
        SELECT_AD_CONVERSATION_INDICES("SELECT convid, visibility, latestmsgid FROM mb_ad_conversation_idx WHERE pbid = ? AND adid = ?"),
        SELECT_CONVERSATIONS("SELECT convid, visibility, notifynew, meparticipant, otherparticipant, adid, latestmsgprev" +
                " FROM mb_conversations WHERE pbid = ? AND convid IN ?"),
        SELECT_CONVERSATIONS_UNREAD_COUNTS("SELECT convid, unread FROM mb_conversation_unread_counts WHERE pbid = ?"),

        SELECT_AD_IDS("SELECT convid, adid FROM mb_conversations WHERE pbid = ? AND convid IN ?"),

        // select single conversation + messages
        SELECT_CONVERSATION("SELECT convid, adid, visibility, notifynew, meparticipant, otherparticipant, latestmsgprev FROM mb_conversations WHERE pbid = ? AND convid = ?"),
        SELECT_CONVERSATION_UNREAD_COUNT("SELECT unread FROM mb_conversation_unread_counts WHERE pbid = ? AND convid = ?"),
        SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR("SELECT msgid, msg, senderid, type FROM mb_messages WHERE pbid = ? AND convid = ? LIMIT ?"),
        SELECT_CONVERSATION_MESSAGES_WITH_CURSOR("SELECT msgid, msg, senderid, type FROM mb_messages WHERE pbid = ? AND convid = ? AND msgid < ? LIMIT ?"),

        // update a single conversation when a new message comes in
        UPDATE_CONVERSATION_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unread = ? WHERE pbid = ? AND convid = ?", true),
        UPDATE_AD_CONVERSATION_UNREAD_COUNT("UPDATE mb_ad_conversation_unread_counts SET unread = ? WHERE pbid = ? AND adid = ? AND convid = ?", true),

        UPDATE_AD_CONVERSATION_INDEX("UPDATE mb_ad_conversation_idx SET visibility = ?, latestmsgid = ? WHERE pbid = ? AND adid = ? AND convid = ?", true),
        UPDATE_CONVERSATION_LATEST_MESSAGE("UPDATE mb_conversations SET visibility = ?, latestmsgprev = ? WHERE pbid = ? AND convid = ?", true),
        UPDATE_CONVERSATION("UPDATE mb_conversations SET adid = ?, visibility = ?, notifynew = ?, meparticipant = ?, otherparticipant = ?, latestmsgprev = ? WHERE pbid = ? AND convid = ?", true),
        INSERT_MESSAGE("INSERT INTO mb_messages (pbid, convid, msgid, msg, senderid, type) VALUES (?, ?, ?, ?, ?, ?)", true),

        CHANGE_CONVERSATION_VISIBILITY("UPDATE mb_conversations SET visibility = ? WHERE pbid = ? AND convid = ?", true),
        CHANGE_CONVERSATION_IDX_VISIBILITY("UPDATE mb_ad_conversation_idx SET visibility = ? WHERE pbid = ? AND adid = ? AND convid = ?", true),

        // block/un-block user
        BLOCK_USER("INSERT INTO mb_blocked_users (blockerid, blockeeid, blockdate) VALUES (?, ?, dateof(now()))", true),
        UNBLOCK_USER("DELETE FROM mb_blocked_users WHERE blockerid = ? AND blockeeid = ?", true),

        // select blocked user
        SELECT_BLOCKED_USER("SELECT blockerid, blockeeid, blockdate FROM mb_blocked_users WHERE blockerid = ? AND blockeeid = ?"),

        // cleanup of old messages and conversations
        INSERT_AD_CONVERSATION_MODIFICATION_IDX("INSERT INTO mb_ad_conversation_modification_idx (pbid, convid, msgid, adid) VALUES (?, ?, ?, ?)", true),
        INSERT_CONVERSATION_MODIFICATION_IDX_BY_DATE("INSERT INTO mb_conversation_modification_idx_by_date (modifdate, msgid, pbid, convid) VALUES (?, ?, ?, ?)", true),

        SELECT_CONVERSATION_MODIFICATION_IDX_BY_DATE("SELECT pbid, convid, msgid FROM mb_conversation_modification_idx_by_date WHERE modifdate = ?"),
        SELECT_AD_CONVERSATION_MODIFICATION_IDXS("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE pbid = ? AND convid = ?"),
        SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE pbid = ? AND convid = ? LIMIT 1"),

        DELETE_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_conversation_unread_counts WHERE pbid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_ad_conversation_unread_counts WHERE pbid = ? AND adid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_INDEX("DELETE FROM mb_ad_conversation_idx WHERE pbid = ? AND adid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION("DELETE FROM mb_conversations WHERE pbid = ? AND convid = ?", true),
        DELETE_CONVERSATION_MESSAGES("DELETE FROM mb_messages WHERE pbid = ? AND convid = ?", true),

        DELETE_AD_CONVERSATION_MODIFICATION_IDX("DELETE FROM mb_ad_conversation_modification_idx WHERE pbid = ? AND convid = ? AND msgid = ?", true),
        DELETE_AD_CONVERSATION_MODIFICATION_IDXS("DELETE FROM mb_ad_conversation_modification_idx WHERE pbid = ? AND convid = ?", true),
        DELETE_CONVERSATION_MODIFICATION_IDX_BY_DATE("DELETE FROM mb_conversation_modification_idx_by_date WHERE modifdate = ? AND msgid = ? AND pbid = ? AND convid = ?", true);

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
                    .setConsistencyLevel(getConsistencyLevel(repository));
        }

        private ConsistencyLevel getConsistencyLevel(DefaultCassandraPostBoxRepository repository) {
            return modifying ? repository.getWriteConsistency() : repository.getReadConsistency();
        }
    }
}