package com.ecg.messagebox.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static org.joda.time.DateTime.now;

@Component("newCassandraPostBoxService")
public class CassandraPostBoxService implements PostBoxService {
    private final CassandraPostBoxRepository postBoxRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final BlockUserRepository blockUserRepository;
    private final UserIdentifierService userIdentifierService;

    private final Timer processNewMessageTimer = newTimer("postBoxService.v2.processNewMessage");
    private final Timer getConversationTimer = newTimer("postBoxService.v2.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxService.v2.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxService.v2.getConversations");
    private final Timer changeConversationVisibilitiesTimer = newTimer("postBoxService.v2.changeConversationVisibilities");
    private final Timer getUnreadCountsTimer = newTimer("postBoxService.v2.getUnreadCounts");
    private final Timer getResponseDataTimer = newTimer("postBoxService.v2.getResponseData");

    private final Counter newConversationCounter = newCounter("postBoxService.v2.newConversationCounter");

    @Autowired
    public CassandraPostBoxService(
            CassandraPostBoxRepository postBoxRepository,
            UserIdentifierService userIdentifierService,
            BlockUserRepository blockUserRepository
    ) {
        this.postBoxRepository = postBoxRepository;
        this.userIdentifierService = userIdentifierService;
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
        this.blockUserRepository = blockUserRepository;
    }

    @Override
    public void processNewMessage(String userId,
                                  com.ecg.replyts.core.api.model.conversation.Conversation rtsConversation,
                                  com.ecg.replyts.core.api.model.conversation.Message rtsMessage,
                                  boolean isNewReply
    ) {
        try (Timer.Context ignored = processNewMessageTimer.time()) {
            String senderUserId = getMessageSenderUserId(rtsConversation, rtsMessage);
            String receiverUserId = getMessageReceiverUserId(rtsConversation, rtsMessage);

            if (!blockUserRepository.areUsersBlocked(senderUserId, receiverUserId)) {
                String messageTypeStr = rtsMessage.getHeaders().getOrDefault("X-Message-Type", MessageType.EMAIL.getValue()).toLowerCase();
                MessageType messageType = MessageType.get(messageTypeStr);
                String messageText = messageResponseFactory.getCleanedMessage(rtsConversation, rtsMessage);
                String customData = rtsMessage.getHeaders().get("X-Message-Metadata");
                String messageIdStr = rtsMessage.getHeaders().get("X-Message-Id");
                UUID messageId = messageIdStr != null ? UUID.fromString(messageIdStr) : rtsMessage.getEventTimeUUID().get();
                Message newMessage = new Message(messageId, messageText, senderUserId, messageType, customData);
                Optional<MessageNotification> messageNotificationOpt = postBoxRepository.getConversationMessageNotification(userId, rtsConversation.getId());

                if (messageNotificationOpt.isPresent()) {
                    boolean notifyAboutNewMessage = isNewReply && messageNotificationOpt.get() == MessageNotification.RECEIVE;
                    postBoxRepository.addMessage(userId, rtsConversation.getId(), rtsConversation.getAdId(), newMessage, notifyAboutNewMessage);
                } else {
                    ConversationThread newConversation = new ConversationThread(
                            rtsConversation.getId(),
                            rtsConversation.getAdId(),
                            Visibility.ACTIVE,
                            MessageNotification.RECEIVE,
                            getParticipants(rtsConversation),
                            newMessage,
                            new ConversationMetadata(now(), rtsConversation.getMessages().get(0).getHeaders().get("Subject")));

                    postBoxRepository.createConversation(userId, newConversation, newMessage, isNewReply);
                    newConversationCounter.inc();
                }
            }
        }
    }

    @Override
    public Optional<ConversationThread> getConversation(String userId, String
            conversationId, Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = getConversationTimer.time()) {
            return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursorOpt, messagesLimit);
        }
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId,
                                                               Optional<String> messageIdCursorOpt, int messagesLimit) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {
            return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursorOpt, messagesLimit).map(conversation -> {
                postBoxRepository.resetConversationUnreadCount(userId, conversationId, conversation.getAdId());
                return new ConversationThread(conversation).addNumUnreadMessages(0);
            });
        }
    }

    @Override
    public PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        try (Timer.Context ignored = getConversationsTimer.time()) {
            return postBoxRepository.getPostBox(userId, visibility, conversationsOffset, conversationsLimit);
        }
    }

    @Override
    public PostBox changeConversationVisibilities(
            String userId, List<String> conversationIds, Visibility newVis, Visibility returnVis, int conversationsOffset, int conversationsLimit
    ) {
        try (Timer.Context ignored = changeConversationVisibilitiesTimer.time()) {
            Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);
            postBoxRepository.changeConversationVisibilities(userId, conversationAdIdsMap, newVis);
            return postBoxRepository.getPostBox(userId, returnVis, conversationsOffset, conversationsLimit).removeConversations(conversationIds);
        }
    }

    @Override
    public UserUnreadCounts getUnreadCounts(String userId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {
            return postBoxRepository.getUserUnreadCounts(userId);
        }
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {

            return postBoxRepository.getResponseData(userId);
        }
    }

    private List<Participant> getParticipants(Conversation rtsConversation) {
        return new ArrayList<Participant>(2) {{
            add(new Participant(getBuyerUserId(rtsConversation), customValue(rtsConversation, "buyer-name"), rtsConversation.getBuyerId(), ParticipantRole.BUYER));
            add(new Participant(getSellerUserId(rtsConversation), customValue(rtsConversation, "seller-name"), rtsConversation.getSellerId(), ParticipantRole.SELLER));
        }};
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