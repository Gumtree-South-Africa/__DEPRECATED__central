package com.ecg.messagebox.persistence.cassandra;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.json.JsonConverter;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.persistence.cassandra.model.ConversationIndex;
import com.ecg.messagebox.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagebox.util.InstrumentedExecutorService;
import com.ecg.messagebox.util.StreamUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.datastax.driver.core.utils.UUIDs.unixTimestamp;
import static com.ecg.messagebox.model.Visibility.get;
import static com.ecg.messagebox.util.uuid.UUIDComparator.staticCompare;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static com.google.common.collect.Lists.newArrayList;

@Component(("newCassandraPostBoxRepo"))
public class DefaultCassandraPostBoxRepository implements CassandraPostBoxRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCassandraPostBoxRepository.class);

    private final Session session;
    private final ConsistencyLevel readConsistency;
    private final ConsistencyLevel writeConsistency;

    private Map<Statements, PreparedStatement> preparedStatements;

    private final ExecutorService executorService;

    private final JsonConverter jsonConverter;

    private final int timeoutInMs;

    private final Timer getPaginatedConversationIdsTimer = newTimer("cassandra.postBoxRepo.v2.getPaginatedConversationIds");
    private final Timer getPostBoxTimer = newTimer("cassandra.postBoxRepo.v2.getPostBox");
    private final Timer getConversationTimer = newTimer("cassandra.postBoxRepo.v2.getConversation");
    private final Timer getConversationWithMessagesTimer = newTimer("cassandra.postBoxRepo.v2.getConversationWithMessages");
    private final Timer getConversationMessagesTimer = newTimer("cassandra.postBoxRepo.v2.getConversationMessages");
    private final Timer createConversationTimer = newTimer("cassandra.postBoxRepo.v2.createConversation");
    private final Timer addMessageTimer = newTimer("cassandra.postBoxRepo.v2.addMessage");
    private final Timer getConversationAdIdsMapTimer = newTimer("cassandra.postBoxRepo.v2.getConversationAdIdsMap");
    private final Timer resetConversationUnreadCountTimer = newTimer("cassandra.postBoxRepo.v2.resetConversationUnreadCount");
    private final Timer changeConversationVisibilitiesTimer = newTimer("cassandra.postBoxRepo.v2.changeConversationVisibilities");
    private final Timer getPostBoxUnreadCountsTimer = newTimer("cassandra.postBoxRepo.v2.getPostBoxUnreadCounts");
    private final Timer getConversationUnreadCountMapTimer = newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCountMap");
    private final Timer getConversationUnreadCountTimer = newTimer("cassandra.postBoxRepo.v2.getConversationUnreadCount");
    private final Timer blockUserTimer = newTimer("cassandra.postBoxRepo.v2.blockUser");
    private final Timer unblockUserTimer = newTimer("cassandra.postBoxRepo.v2.unblockUser");
    private final Timer getBlockedUserInfoTimer = newTimer("cassandra.postBoxRepo.v2.getBlockedUserInfo");
    private final Timer deleteConversationsTimer = newTimer("cassandra.postBoxRepo.v2.deleteConversations");
    private final Timer deleteConversationTimer = newTimer("cassandra.postBoxRepo.v2.deleteConversation");
    private final Timer deleteModificationIndexTimer = newTimer("cassandra.postBoxRepo.v2.deleteModificationIndexByDate");
    private final Timer getConversationModificationsByDateTimer = newTimer("cassandra.postBoxRepo.v2.getConversationModificationsByHour");
    private final Timer getCronJobLastProcessedDateTimer = newTimer("cassandra.postBoxRepo.v2.getCronJobLastProcessedDateTimer");
    private final Timer setCronJobLastProcessedDateTimer = newTimer("cassandra.postBoxRepo.v2.setCronJobLastProcessedDateTimer");


    private final Counter postBoxFutureFailures = newCounter("cassandra.postBoxRepo.v2.getPostBox-futureFailures");
    private final Counter conversationFutureFailures = newCounter("cassandra.postBoxRepo.v2.getConversationWithMessages-futureFailures");
    private final Counter blockedUsersFutureFailures = newCounter("cassandra.postBoxRepo.v2.getBlockedUserInfo-futureFailures");

    @Autowired
    public DefaultCassandraPostBoxRepository(@Qualifier("cassandraSession") Session session,
                                             @Qualifier("cassandraReadConsistency") ConsistencyLevel readConsistency,
                                             @Qualifier("cassandraWriteConsistency") ConsistencyLevel writeConsistency,
                                             JsonConverter jsonConverter,
                                             @Value("${messagebox.future.timeout.ms:5000}") int timeoutInMs,
                                             @Value("${messagebox.convRepo.execSrv.corePoolSize:5}") int corePoolSize,
                                             @Value("${messagebox.convRepo.execSrv.maxPoolSize:50}") int maxPoolSize) {
        this.session = session;
        this.readConsistency = readConsistency;
        this.writeConsistency = writeConsistency;

        String metricsOwner = "newCassandraPostBoxRepo";
        String metricsName = "execSrv";
        this.executorService = new InstrumentedExecutorService(
                new ThreadPoolExecutor(
                        corePoolSize, maxPoolSize,
                        60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)
                ),
                metricsOwner,
                metricsName);

        this.timeoutInMs = timeoutInMs;

        this.jsonConverter = jsonConverter;

        preparedStatements = Statements.prepare(session);
    }

    @Override
    public PostBox getPostBox(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = getPostBoxTimer.time()) {

            Future<List<String>> conversationIdsFuture =
                    executorService.submit(() -> getPaginatedConversationIds(userId, visibility, conversationsOffset, conversationsLimit));
            Future<Map<String, Integer>> unreadCountsFuture =
                    executorService.submit(() -> getConversationUnreadCountMap(userId));

            try {
                List<String> conversationIds = conversationIdsFuture.get(timeoutInMs, TimeUnit.SECONDS);
                ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS.bind(this, userId, conversationIds));

                Map<String, Integer> conversationUnreadCountsMap = unreadCountsFuture.get(timeoutInMs, TimeUnit.SECONDS);

                List<ConversationThread> conversations = StreamUtils.toStream(resultSet)
                        .map(row -> {
                            ConversationThread conversation = createConversation(userId, row);
                            conversation.addNumUnreadMessages(conversationUnreadCountsMap.getOrDefault(row.getString("convid"), 0));
                            return conversation;
                        })
                        .sorted((c1, c2) -> staticCompare(c2.getLatestMessage().getId(), c1.getLatestMessage().getId()))
                        .collect(Collectors.toList());

                PostBoxUnreadCounts postBoxUnreadCounts = createPostBoxUnreadCounts(userId, conversationUnreadCountsMap.values());

                return new PostBox(userId, conversations, postBoxUnreadCounts);

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                conversationIdsFuture.cancel(true);
                unreadCountsFuture.cancel(true);
                postBoxFutureFailures.inc();
                LOGGER.error("Could not fetch conversation indices for user id {} and visibility {}", userId, visibility.name(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> getPaginatedConversationIds(String userId, Visibility visibility, int offset, int limit) {
        try (Timer.Context ignored = getPaginatedConversationIdsTimer.time()) {

            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATION_INDICES.bind(this, userId));

            return StreamUtils.toStream(resultSet)
                    .map(row -> new ConversationIndex(row.getString("convid"),
                            row.getString("adid"),
                            get(row.getInt("vis")),
                            row.getUUID("latestmsgid")))
                    .filter(ci -> ci.getVisibility() == visibility)
                    .sorted((ci1, ci2) -> staticCompare(ci2.getLatestMessageId(), ci1.getLatestMessageId()))
                    .skip(offset * limit)
                    .limit(limit)
                    .map(ConversationIndex::getConversationId)
                    .collect(Collectors.toList());
        }
    }

    private Map<String, Integer> getConversationUnreadCountMap(String userId) {
        try (Timer.Context ignored = getConversationUnreadCountMapTimer.time()) {

            ResultSet resultSet = session.execute(Statements.SELECT_CONVERSATIONS_UNREAD_COUNTS.bind(this, userId));
            return StreamUtils.toStream(resultSet)
                    .collect(Collectors.toMap(
                            row -> row.getString("convid"),
                            row -> row.getInt("unread")));
        }
    }

    @Override
    public Optional<ConversationThread> getConversation(String userId, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            Row row = session.execute(Statements.SELECT_CONVERSATION.bind(this, userId, conversationId)).one();
            return row != null ? Optional.of(createConversation(userId, row)) : Optional.empty();
        }
    }

    @Override
    public Optional<ConversationThread> getConversationWithMessages(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = getConversationWithMessagesTimer.time()) {

            Future<Integer> unreadMessagesCountFuture = executorService.submit(() -> getConversationUnreadCount(userId, conversationId));
            Future<Row> conversationFuture = executorService.submit(() -> session.execute(Statements.SELECT_CONVERSATION.bind(this, userId, conversationId)).one());
            Future<List<Message>> messagesFuture = executorService.submit(() -> getConversationMessages(userId, conversationId, messageIdCursorOpt, messagesLimit));

            try {
                Row row = conversationFuture.get(timeoutInMs, TimeUnit.SECONDS);
                if (row != null) {
                    ConversationThread conversation = createConversation(userId, row);

                    int unreadMessagesCount = unreadMessagesCountFuture.get(timeoutInMs, TimeUnit.SECONDS);
                    conversation.addNumUnreadMessages(unreadMessagesCount);

                    List<Message> messages = messagesFuture.get(timeoutInMs, TimeUnit.SECONDS);
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
                conversationFutureFailures.inc();
                LOGGER.error("Could not fetch conversation for user identifier {} and conversation id {}", userId, conversationId, e);
                throw new RuntimeException(e);
            }
        }
    }

    private List<Message> getConversationMessages(String userId, String conversationId, Optional<String> cursorOpt, int limit) {
        try (Timer.Context ignored = getConversationMessagesTimer.time()) {

            ResultSet resultSet = cursorOpt
                    .map(cursor -> session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITH_CURSOR.bind(this, userId, conversationId, UUID.fromString(cursor), limit)))
                    .orElse(session.execute(Statements.SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR.bind(this, userId, conversationId, limit)));

            return StreamUtils.toStream(resultSet)
                    .map(row -> {
                        UUID messageId = row.getUUID("msgid");
                        MessageMetadata metadata = jsonConverter.fromMessageMetadataJson(userId, conversationId, messageId.toString(), row.getString("metadata"));
                        return new Message(row.getUUID("msgid"),
                                MessageType.get(row.getString("type")),
                                metadata);
                    })
                    .sorted((m1, m2) -> staticCompare(m1.getId(), m2.getId()))
                    .collect(Collectors.toList());
        }
    }

    private ConversationThread createConversation(String userId, Row row) {
        String conversationId = row.getString("convid");
        String adId = row.getString("adid");

        Visibility visibility = get(row.getInt("vis"));
        MessageNotification messageNotification = MessageNotification.get(row.getInt("ntfynew"));
        List<Participant> participants = jsonConverter.fromParticipantsJson(userId, conversationId, row.getString("participants"));
        Message latestMessage = jsonConverter.fromMessageJson(userId, conversationId, row.getString("latestmsg"));
        ConversationMetadata metadata = jsonConverter.fromConversationMetadataJson(userId, conversationId, row.getString("metadata"));

        return new ConversationThread(conversationId, adId, visibility, messageNotification, participants, latestMessage, metadata);
    }

    @Override
    public PostBoxUnreadCounts getPostBoxUnreadCounts(String userId) {
        try (Timer.Context ignored = getPostBoxUnreadCountsTimer.time()) {

            ResultSet result = session.execute(Statements.SELECT_POSTBOX_UNREAD_COUNTS.bind(this, userId));
            List<Integer> conversationUnreadCounts = StreamUtils.toStream(result)
                    .map(row -> row.getInt("unread"))
                    .collect(Collectors.toList());
            return createPostBoxUnreadCounts(userId, conversationUnreadCounts);
        }
    }

    private PostBoxUnreadCounts createPostBoxUnreadCounts(String userId, Collection<Integer> conversationUnreadCounts) {
        CountersConsumer counters = conversationUnreadCounts.stream()
                .collect(CountersConsumer::new, CountersConsumer::add, CountersConsumer::reduce);
        return new PostBoxUnreadCounts(userId, counters.numUnreadConversations, counters.numUnreadMessages);
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
    public void resetConversationUnreadCount(String userId, String conversationId, String adId) {
        try (Timer.Context ignored = resetConversationUnreadCountTimer.time()) {

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, conversationId));
            batch.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, 0, userId, adId, conversationId));

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void createConversation(String userId, ConversationThread conversation, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = createConversationTimer.time()) {

            String conversationId = conversation.getId();
            String participantsJson = jsonConverter.toParticipantsJson(userId, conversationId, conversation.getParticipants());
            String messageJson = jsonConverter.toMessageJson(userId, conversationId, message);
            String metadataJson = jsonConverter.toConversationMetadataJson(userId, conversationId, conversation.getMetadata());

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION.bind(this, conversation.getAdId(), Visibility.ACTIVE.getCode(), MessageNotification.RECEIVE.getCode(),
                    participantsJson, messageJson, metadataJson, userId, conversationId));
            newMessageCqlStatements(userId, conversationId, conversation.getAdId(), message, incrementUnreadCount).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void addMessage(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        try (Timer.Context ignored = addMessageTimer.time()) {

            String messageJson = jsonConverter.toMessageJson(userId, conversationId, message);

            BatchStatement batch = new BatchStatement();

            batch.add(Statements.UPDATE_CONVERSATION_LATEST_MESSAGE.bind(this, Visibility.ACTIVE.getCode(), messageJson, userId, conversationId));
            newMessageCqlStatements(userId, conversationId, adId, message, incrementUnreadCount).forEach(batch::add);

            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    private List<Statement> newMessageCqlStatements(String userId, String conversationId, String adId, Message message, boolean incrementUnreadCount) {
        List<Statement> statements = new ArrayList<>();

        statements.add(Statements.UPDATE_AD_CONVERSATION_INDEX.bind(this, Visibility.ACTIVE.getCode(), message.getId(), userId, adId, conversationId));

        String messageMetadata = jsonConverter.toMessageMetadataJson(userId, conversationId, message);
        statements.add(Statements.INSERT_MESSAGE.bind(this, userId, conversationId,
                message.getId(), message.getType().getValue(), messageMetadata));

        if (incrementUnreadCount) {
            int newUnreadCount = getConversationUnreadCount(userId, conversationId) + 1;
            statements.add(Statements.UPDATE_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, conversationId));
            statements.add(Statements.UPDATE_AD_CONVERSATION_UNREAD_COUNT.bind(this, newUnreadCount, userId, adId, conversationId));
        }

        statements.add(Statements.INSERT_AD_CONVERSATION_MODIFICATION_IDX.bind(this, userId, conversationId, message.getId(), adId));
        Date modifiedDate = message.getReceivedDate().hourOfDay().roundFloorCopy().toDate();
        statements.add(Statements.INSERT_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, modifiedDate, message.getId(), userId, conversationId));

        return statements;
    }

    @Override
    public void changeConversationVisibilities(String userId, Map<String, String> conversationAdIdsMap, Visibility visibility) {
        try (Timer.Context ignored = changeConversationVisibilitiesTimer.time()) {

            BatchStatement batch = new BatchStatement();
            conversationAdIdsMap.forEach((conversationId, adId) -> {
                batch.add(Statements.CHANGE_CONVERSATION_VISIBILITY.bind(this, visibility.getCode(), userId, conversationId));
                batch.add(Statements.CHANGE_CONVERSATION_IDX_VISIBILITY.bind(this, visibility.getCode(), userId, adId, conversationId));
            });
            batch.setConsistencyLevel(getWriteConsistency());
            session.execute(batch);
        }
    }

    @Override
    public void deleteConversations(String userId, Map<String, String> adConversationIdsMap) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            adConversationIdsMap.forEach((adId, conversationId) -> {
                BatchStatement batch = new BatchStatement();
                batch.add(Statements.DELETE_AD_CONVERSATION.bind(this, userId, conversationId));
                batch.add(Statements.DELETE_AD_CONVERSATION_INDEX.bind(this, userId, adId, conversationId));
                batch.add(Statements.DELETE_CONVERSATION_UNREAD_COUNT.bind(this, userId, conversationId));
                batch.add(Statements.DELETE_AD_CONVERSATION_UNREAD_COUNT.bind(this, userId, adId, conversationId));
                batch.add(Statements.DELETE_CONVERSATION_MESSAGES.bind(this, userId, conversationId));
                batch.setConsistencyLevel(getWriteConsistency());
                session.execute(batch);

                ResultSet resultSet = session.execute(Statements.SELECT_AD_CONVERSATION_MODIFICATION_IDXS.bind(this, userId, conversationId));
                StreamUtils.toStream(resultSet)
                        .map(row -> row.getUUID("msgid"))
                        .forEach(messageId -> {
                            Date modifiedDate = new DateTime(unixTimestamp(messageId)).hourOfDay().roundFloorCopy().toDate();
                            session.execute(Statements.DELETE_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, modifiedDate, messageId, userId, conversationId));
                        });
                session.execute(Statements.DELETE_AD_CONVERSATION_MODIFICATION_IDXS.bind(this, userId, conversationId));
            });
        }
    }

    @Override
    public void deleteConversation(String userId, String adId, String conversationId) {
        try (Timer.Context ignored = deleteConversationTimer.time()) {
            deleteConversations(userId, Collections.singletonMap(adId, conversationId));
        }
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
    public Stream<ConversationModification> getConversationModificationsByHour(DateTime date) {
        try (Timer.Context ignored = getConversationModificationsByDateTimer.time()) {
            Statement bound = Statements.SELECT_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, date.hourOfDay().roundFloorCopy().toDate());
            ResultSet resultSet = session.execute(bound);
            return StreamUtils.toStream(resultSet)
                    .map(row -> new ConversationModification(row.getString("usrid"), row.getString("convid"), row.getUUID("msgid"), date));
        }
    }

    @Override
    public void deleteModificationIndexByDate(DateTime modifiedDate, UUID messageId, String userId, String conversationId) {
        try (Timer.Context ignored = deleteModificationIndexTimer.time()) {
            session.execute(Statements.DELETE_CONVERSATION_MODIFICATION_IDX_BY_DATE.bind(this, modifiedDate.toDate(), messageId, userId, conversationId));
        }
    }

    @Override
    public void blockUser(String reporterUserId, String userIdToBlock) {
        try (Timer.Context ignored = blockUserTimer.time()) {
            session.execute(Statements.BLOCK_USER.bind(this, reporterUserId, userIdToBlock, DateTime.now().toDate()));
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
                    Row row = future.get(timeoutInMs, TimeUnit.SECONDS).one();
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
                    blockedUsersFutureFailures.inc();
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

    @Override
    public ConversationModification getLastConversationModification(String userId, String convId) {
        try (Timer.Context ignored = getCronJobLastProcessedDateTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX.bind(this, userId, convId));
            Row row = resultSet.one();
            if (row == null) {
                return null;
            }

            String adId = row.getString("adid");
            UUID msgId = row.getUUID("msgid");
            DateTime lastModifiedDate = new DateTime(unixTimestamp(msgId));

            return new ConversationModification(userId, convId, adId, msgId, lastModifiedDate);
        }
    }

    @Override
    public DateTime getCronjobLastProcessedDate(String jobName){
        try (Timer.Context ignored = getCronJobLastProcessedDateTimer.time()) {
            ResultSet resultSet = session.execute(Statements.SELECT_CRONJOB_LAST_PROCESSED_DATE.bind(this, jobName));
            Row row = resultSet.one();
            if (row == null) {
                return null;
            }
            return new DateTime(row.getDate("last_processed_date").getTime());
        }
    }

    @Override
    public void setCronjobLastProcessedDate(String jobName, DateTime lastProcessed){
        try (Timer.Context ignored = setCronJobLastProcessedDateTimer.time()) {
            session.execute(Statements.INSERT_CRONJOB_LAST_PROCESSED_DATE.bind(this, jobName, lastProcessed.toDate()));
        }
    }

    public ConsistencyLevel getReadConsistency() {
        return readConsistency;
    }

    public ConsistencyLevel getWriteConsistency() {
        return writeConsistency;
    }

    enum Statements {

        // select the user's unread counts
        SELECT_POSTBOX_UNREAD_COUNTS("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ?"),

        // select the ad's unread counts
        SELECT_AD_UNREAD_COUNT("SELECT unread FROM mb_ad_conversation_unread_counts WHERE usrid = ? and adid = ?"),

        // select the user's conversations
        SELECT_CONVERSATION_INDICES("SELECT convid, adid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrid = ?"),
        SELECT_AD_CONVERSATION_INDICES("SELECT convid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrId = ? AND adid = ?"),
        SELECT_CONVERSATIONS("SELECT convid, vis, ntfynew, participants, adid, latestmsg, metadata" +
                " FROM mb_conversations WHERE usrid = ? AND convid IN ?"),
        SELECT_CONVERSATIONS_UNREAD_COUNTS("SELECT convid, unread FROM mb_conversation_unread_counts WHERE usrid = ?"),

        SELECT_AD_IDS("SELECT convid, adid FROM mb_conversations WHERE usrid = ? AND convid IN ?"),

        // select single conversation + messages
        SELECT_CONVERSATION("SELECT convid, adid, vis, ntfynew, participants, latestmsg, metadata FROM mb_conversations WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_UNREAD_COUNT("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?"),
        SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? LIMIT ?"),
        SELECT_CONVERSATION_MESSAGES_WITH_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? AND msgid < ? LIMIT ?"),

        // update a single conversation when a new message comes in
        UPDATE_CONVERSATION_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unread = ? WHERE usrid = ? AND convid = ?", true),
        UPDATE_AD_CONVERSATION_UNREAD_COUNT("UPDATE mb_ad_conversation_unread_counts SET unread = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

        UPDATE_AD_CONVERSATION_INDEX("UPDATE mb_ad_conversation_idx SET vis = ?, latestmsgid = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),
        UPDATE_CONVERSATION_LATEST_MESSAGE("UPDATE mb_conversations SET vis = ?, latestmsg = ? WHERE usrid = ? AND convid = ?", true),
        UPDATE_CONVERSATION("UPDATE mb_conversations SET adid = ?, vis = ?, ntfynew = ?, participants = ?, latestmsg = ?, metadata = ? WHERE usrid = ? AND convid = ?", true),
        INSERT_MESSAGE("INSERT INTO mb_messages (usrid, convid, msgid, type, metadata) VALUES (?, ?, ?, ?, ?)", true),

        CHANGE_CONVERSATION_VISIBILITY("UPDATE mb_conversations SET vis = ? WHERE usrid = ? AND convid = ?", true),
        CHANGE_CONVERSATION_IDX_VISIBILITY("UPDATE mb_ad_conversation_idx SET vis = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

        // block/un-block user
        BLOCK_USER("INSERT INTO mb_blocked_users (blockerid, blockeeid, blockdate) VALUES (?, ?, ?)", true),
        UNBLOCK_USER("DELETE FROM mb_blocked_users WHERE blockerid = ? AND blockeeid = ?", true),

        // select blocked user
        SELECT_BLOCKED_USER("SELECT blockerid, blockeeid, blockdate FROM mb_blocked_users WHERE blockerid = ? AND blockeeid = ?"),

        // cleanup of old messages and conversations
        INSERT_AD_CONVERSATION_MODIFICATION_IDX("INSERT INTO mb_ad_conversation_modification_idx (usrid, convid, msgid, adid) VALUES (?, ?, ?, ?)", true),
        INSERT_CONVERSATION_MODIFICATION_IDX_BY_DATE("INSERT INTO mb_conversation_modification_idx_by_date (modifdate, msgid, usrid, convid) VALUES (?, ?, ?, ?)", true),

        SELECT_CONVERSATION_MODIFICATION_IDX_BY_DATE("SELECT usrid, convid, msgid FROM mb_conversation_modification_idx_by_date WHERE modifdate = ?"),
        SELECT_AD_CONVERSATION_MODIFICATION_IDXS("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?"),
        SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? LIMIT 1"),

        DELETE_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_ad_conversation_unread_counts WHERE usrid = ? AND adid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION_INDEX("DELETE FROM mb_ad_conversation_idx WHERE usrid = ? AND adid = ? AND convid = ?", true),
        DELETE_AD_CONVERSATION("DELETE FROM mb_conversations WHERE usrid = ? AND convid = ?", true),
        DELETE_CONVERSATION_MESSAGES("DELETE FROM mb_messages WHERE usrid = ? AND convid = ?", true),

        DELETE_AD_CONVERSATION_MODIFICATION_IDX("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? AND msgid = ?", true),
        DELETE_AD_CONVERSATION_MODIFICATION_IDXS("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?", true),
        DELETE_CONVERSATION_MODIFICATION_IDX_BY_DATE("DELETE FROM mb_conversation_modification_idx_by_date WHERE modifdate = ? AND msgid = ? AND usrid = ? AND convid = ?", true),

        SELECT_CRONJOB_LAST_PROCESSED_DATE("SELECT last_processed_date FROM mb_cronjob_clock WHERE job_name = ?"),
        INSERT_CRONJOB_LAST_PROCESSED_DATE("INSERT INTO mb_cronjob_clock (job_name, last_processed_date) VALUES (?,?)");


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