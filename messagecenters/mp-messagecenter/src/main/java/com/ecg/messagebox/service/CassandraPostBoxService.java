package com.ecg.messagebox.service;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagebox.util.InstrumentedExecutorService;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static java.lang.Runtime.getRuntime;

@Component("newCassandraPostBoxService")
public class CassandraPostBoxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPostBoxService.class);

    private final CassandraPostBoxRepository postBoxRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;

    private final ExecutorService executorService;

    private final int timeoutInMs;

    private final Timer processNewMessageTimer = newTimer("postBoxService.v2.processNewMessage");
    private final Timer getConversationTimer = newTimer("postBoxService.v2.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxService.v2.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxService.v2.getConversations");
    private final Timer changeConversationVisibilitiesTimer = newTimer("postBoxService.v2.changeConversationVisibilities");
    private final Timer getUnreadCountsTimer = newTimer("postBoxService.v2.getUnreadCounts");

    @Autowired
    public CassandraPostBoxService(CassandraPostBoxRepository postBoxRepository,
                                   UserIdentifierService userIdentifierService,
                                   @Value("${messagebox.future.timeout.ms:5000}") int timeoutInMs) {
        this.postBoxRepository = postBoxRepository;
        this.userIdentifierService = userIdentifierService;
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
        this.timeoutInMs = timeoutInMs;

        String metricsOwner = "newCassandraPostBoxService";
        String metricsName = "execSrv";
        this.executorService = new InstrumentedExecutorService(
                new ThreadPoolExecutor(
                        0, getRuntime().availableProcessors() * 2,
                        60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)
                ),
                metricsOwner,
                metricsName);
    }

    @Override
    public void processNewMessage(String userId,
                                  com.ecg.replyts.core.api.model.conversation.Conversation rtsConversation,
                                  com.ecg.replyts.core.api.model.conversation.Message rtsMessage,
                                  boolean newReplyArrived) {

        try (Timer.Context ignored = processNewMessageTimer.time()) {

            String senderUserId = getMessageSenderUserId(rtsConversation, rtsMessage);
            String receiverUserId = getMessageReceiverUserId(rtsConversation, rtsMessage);

            Future<Boolean> areUsersBlockedFuture = executorService.submit(() -> postBoxRepository.areUsersBlocked(senderUserId, receiverUserId));
            Future<Optional<ConversationThread>> conversationFuture = executorService.submit(() -> postBoxRepository.getConversation(userId, rtsConversation.getId()));

            try {
                if (areUsersBlockedFuture.get(timeoutInMs, TimeUnit.SECONDS)) {
                    // there is a blocking between the two users, so no new messages will be added to this user's conversation projection
                    return;
                }

                MessageType messageType = MessageType.get(rtsMessage.getHeaders().getOrDefault("X-Message-Type", MessageType.EMAIL.getValue()).toLowerCase());
                String messageText = messageResponseFactory.getCleanedMessage(rtsConversation, rtsMessage);
                Message newMessage = new Message(UUIDs.timeBased(), messageText, senderUserId, messageType);

                Optional<ConversationThread> conversationOpt = conversationFuture.get(timeoutInMs, TimeUnit.SECONDS);

                if (conversationOpt.isPresent()) {
                    ConversationThread conversation = conversationOpt.get();

                    boolean notifyAboutNewMessage = newReplyArrived && conversation.getMessageNotification() == MessageNotification.RECEIVE;

                    postBoxRepository.addMessage(userId, rtsConversation.getId(), rtsConversation.getAdId(), newMessage, notifyAboutNewMessage);
                } else {
                    ConversationThread newConversation = new ConversationThread(
                            rtsConversation.getId(),
                            rtsConversation.getAdId(),
                            Visibility.ACTIVE,
                            MessageNotification.RECEIVE,
                            getParticipants(rtsConversation),
                            newMessage,
                            new ConversationMetadata(rtsMessage.getHeaders().get("Subject")));

                    postBoxRepository.createConversation(userId, newConversation, newMessage, newReplyArrived);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                areUsersBlockedFuture.cancel(true);
                conversationFuture.cancel(true);
                LOGGER.error("Could not process new conversation message for postbox id {}, conversation id {} and RTS message id {}",
                        userId, rtsConversation.getId(), rtsMessage.getId(), e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Optional<ConversationThread> getConversation(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursorOpt, messagesLimit);
        }
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            Optional<ConversationThread> conversationOpt = getConversation(userId, conversationId, messageIdCursorOpt, messagesLimit);
            if (conversationOpt.isPresent()) {
                ConversationThread conversation = conversationOpt.get();
                postBoxRepository.resetConversationUnreadCount(userId, conversationId, conversation.getAdId());
                return Optional.of(new ConversationThread(conversation).addNumUnreadMessages(0));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            return postBoxRepository.getPostBox(userId, visibility, conversationsOffset, conversationsLimit);
        }
    }

    @Override
    public PostBox changeConversationVisibilities(String userId, List<String> conversationIds, Visibility newVis, Visibility returnVis,
                                                  int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = changeConversationVisibilitiesTimer.time()) {

            Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);

            postBoxRepository.changeConversationVisibilities(userId, conversationAdIdsMap, newVis);

            return postBoxRepository.getPostBox(userId, returnVis, conversationsOffset, conversationsLimit)
                    .filterConversations(conversationIds);
        }
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String userId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {

            return postBoxRepository.getPostBoxUnreadCounts(userId);
        }
    }

    private List<Participant> getParticipants(Conversation rtsConversation) {
        List<Participant> participants = new ArrayList<>(2);
        participants.add(
                new Participant(
                        getBuyerUserId(rtsConversation),
                        customValue(rtsConversation, "buyer-name"),
                        rtsConversation.getBuyerId(),
                        ParticipantRole.BUYER));
        participants.add(
                new Participant(
                        getSellerUserId(rtsConversation),
                        customValue(rtsConversation, "seller-name"),
                        rtsConversation.getSellerId(),
                        ParticipantRole.SELLER));
        return participants;
    }

    private String getMessageSenderUserId(Conversation rtsConversation, com.ecg.replyts.core.api.model.conversation.Message rtsMessage) {
        return rtsMessage.getMessageDirection() == BUYER_TO_SELLER ? getBuyerUserId(rtsConversation) : getSellerUserId(rtsConversation);
    }

    private String getMessageReceiverUserId(Conversation rtsConversation, com.ecg.replyts.core.api.model.conversation.Message rtsMessage) {
        return rtsMessage.getMessageDirection() == BUYER_TO_SELLER ? getSellerUserId(rtsConversation) : getBuyerUserId(rtsConversation);
    }

    private String getBuyerUserId(Conversation rtsConversation) {
        return customValue(rtsConversation, userIdentifierService.getBuyerUserIdName());
    }

    private String getSellerUserId(Conversation rtsConversation) {
        return customValue(rtsConversation, userIdentifierService.getSellerUserIdName());
    }

    private String customValue(Conversation rtsConversation, String customValueKey) {
        return rtsConversation.getCustomValues().get(customValueKey);
    }
}