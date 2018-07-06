package com.ecg.messagebox.service;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.controllers.requests.PartnerMessagePayload;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.model.Attachment;
import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.ImmutableAttachment;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageMetadata;
import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static org.joda.time.DateTime.now;

/**
 * TODO: PB: Remove one line methods and remove spaghetti code. Remove dependency on CORE
 * After MessageBox upgrade is done.
 */
@Component
public class CassandraPostBoxService implements PostBoxService {
    private final CassandraPostBoxRepository postBoxRepository;
    private final UserIdentifierService userIdentifierService;
    private final ResponseDataCalculator responseDataCalculator;
    private final MessageAddedEventProcessor messageAddedEventProcessor;
    private final ConversationRepository conversationRepository;

    static final String SYSTEM_MESSAGE_USER_ID = "-1";

    @Autowired
    public CassandraPostBoxService(
            CassandraPostBoxRepository postBoxRepository,
            UserIdentifierService userIdentifierService,
            ResponseDataCalculator responseDataCalculator,
            MessageAddedEventProcessor messageAddedEventProcessor,
            ConversationRepository conversationRepository) {

        this.postBoxRepository = postBoxRepository;
        this.userIdentifierService = userIdentifierService;
        this.responseDataCalculator = responseDataCalculator;
        this.messageAddedEventProcessor = messageAddedEventProcessor;
        this.conversationRepository = conversationRepository;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void processNewMessage(String userId,
                                  com.ecg.replyts.core.api.model.conversation.Conversation rtsConversation,
                                  com.ecg.replyts.core.api.model.conversation.Message rtsMessage,
                                  boolean isNewReply,
                                  String cleanMessageText) {

        String senderUserId = getMessageSenderUserId(rtsConversation, rtsMessage);

        String messageTypeStr = rtsMessage.getHeaders().getOrDefault("X-Message-Type", MessageType.EMAIL.getValue()).toLowerCase();
        MessageType messageType = MessageType.get(messageTypeStr);
        String customData = rtsMessage.getHeaders().get("X-Message-Metadata");
        String messageIdStr = rtsMessage.getHeaders().get("X-Message-ID");
        UUID messageId = messageIdStr != null ? UUID.fromString(messageIdStr) : UUIDs.timeBased();
        List<Attachment> attachments = rtsMessage.getAttachmentFilenames()
                .stream()
                .map(fileName -> ImmutableAttachment.builder()
                        .fileName(fileName)
                        .messageID(rtsMessage.getId())
                        .build())
                .collect(Collectors.toList());
        Message newMessage = new Message(messageId, cleanMessageText, senderUserId, messageType, customData, rtsMessage.getHeaders(), attachments);
        Optional<MessageNotification> messageNotificationOpt = postBoxRepository.getConversationMessageNotification(userId, rtsConversation.getId());

        if (messageNotificationOpt.isPresent()) {
            boolean notifyAboutNewMessage = isNewReply && messageNotificationOpt.get() == MessageNotification.RECEIVE;
            postBoxRepository.addMessage(userId, rtsConversation.getId(), rtsConversation.getAdId(), newMessage, notifyAboutNewMessage);
        } else {
            ConversationThread newConversation = new ConversationThread(
                    rtsConversation.getId(),
                    rtsConversation.getAdId(),
                    userId,
                    Visibility.ACTIVE,
                    MessageNotification.RECEIVE,
                    getParticipants(rtsConversation),
                    newMessage,
                    new ConversationMetadata(
                            now(),
                            rtsConversation.getMessages().get(0).getHeaders().get("Subject"),
                            rtsMessage.getHeaders().get("X-Conversation-Title"),
                            rtsMessage.getHeaders().get("X-Conversation-Image-Url")
                    )
            );

            postBoxRepository.createConversation(userId, newConversation, newMessage, isNewReply);
        }

        responseDataCalculator.storeResponseData(userId, rtsConversation, rtsMessage);
    }

    @Override
    public Optional<ConversationThread> getConversation(String userId, String conversationId, String messageIdCursor, int messagesLimit) {
        return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursor, messagesLimit);
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, String messageIdCursor, int messagesLimit) {
        return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursor, messagesLimit).map(conversation -> {
            List<Participant> participants = conversation.getParticipants();
            String otherParticipantUserId = participants.get(0).getUserId().equals(userId) ? participants.get(1).getUserId() : participants.get(0).getUserId();
            postBoxRepository.resetConversationUnreadCount(userId, otherParticipantUserId, conversationId, conversation.getAdId());
            return new ConversationThread(conversation).addNumUnreadMessages(userId, 0);
        });
    }

    @Override
    public PostBox markConversationsAsRead(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        PostBox postBox = postBoxRepository.getPostBox(userId, visibility, conversationsOffset, conversationsLimit);
        postBoxRepository.resetConversationsUnreadCount(postBox);

        for (ConversationThread conversation : postBox.getConversations()) {
            conversation.addNumUnreadMessages(userId, 0);
        }

        return postBox;
    }

    @Override
    public PostBox getConversations(String userId, Visibility visibility, int conversationsOffset, int conversationsLimit) {
        return postBoxRepository.getPostBox(userId, visibility, conversationsOffset, conversationsLimit);
    }

    @Override
    public PostBox archiveConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit) {
        Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);
        postBoxRepository.archiveConversations(userId, conversationAdIdsMap);
        return postBoxRepository.getPostBox(userId, Visibility.ACTIVE, conversationsOffset, conversationsLimit).removeConversations(conversationIds);
    }

    @Override
    public PostBox activateConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit) {
        Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);
        postBoxRepository.activateConversations(userId, conversationAdIdsMap);
        return postBoxRepository.getPostBox(userId, Visibility.ARCHIVED, conversationsOffset, conversationsLimit).removeConversations(conversationIds);
    }

    @Override
    public UserUnreadCounts getUnreadCounts(String userId) {
        return postBoxRepository.getUserUnreadCounts(userId);
    }

    @Override
    public void deleteConversation(String userId, String conversationId, String adId) {
        postBoxRepository.deleteConversation(userId, conversationId, adId);
    }

    @Override
    public List<String> getConversationsById(String userId, String adId, int amountLimit) {
        return postBoxRepository.resolveConversationIdsByUserIdAndAdId(userId, adId, amountLimit);
    }

    @Override
    public Optional<String> storePartnerMessage(PartnerMessagePayload payload) {
        Participant buyer = payload.getBuyer();
        Participant seller = payload.getSeller();

        List<String> conversationIds = getConversationsById(buyer.getUserId(), payload.getAdId(), 1);
        boolean createNewConversation = conversationIds.isEmpty();
        String conversationId = conversationIds.stream().findAny()
                .orElse(Guids.next());

        MessageDirection messageDirection = getMessageDirection(buyer, seller, payload.getSenderUserId());
        boolean increaseBuyerUnreadCount = messageDirection == MessageDirection.SELLER_TO_BUYER;
        boolean increaseSellerUnreadCount = messageDirection == BUYER_TO_SELLER;
        if (createNewConversation) {
            postBoxRepository.createPartnerConversation(payload, createMessage(payload), conversationId, buyer.getUserId(), increaseBuyerUnreadCount);
            postBoxRepository.createPartnerConversation(payload, createMessage(payload), conversationId, seller.getUserId(), increaseSellerUnreadCount);
        } else {
            postBoxRepository.addMessage(buyer.getUserId(), conversationId, payload.getAdId(), createMessage(payload), increaseBuyerUnreadCount);
            postBoxRepository.addMessage(seller.getUserId(), conversationId, payload.getAdId(), createMessage(payload), increaseSellerUnreadCount);
        }

        return Optional.of(conversationId);
    }

    private static Message createMessage(PartnerMessagePayload payload) {
        return new Message(UUIDs.timeBased(), payload.getType(), new MessageMetadata(payload.getText(), payload.getSenderUserId()));
    }

    @Override
    public void createSystemMessage(String userId, String conversationId, String adId, String text, String customData, boolean sendPush) {
        UUID messageId = UUIDs.timeBased();
        Message systemMessage = new Message(messageId, text, SYSTEM_MESSAGE_USER_ID, MessageType.SYSTEM_MESSAGE, customData);
        postBoxRepository.addSystemMessage(userId, conversationId, adId, systemMessage);

        if (sendPush) {
            /*
             * TODO: PB: Dependency to CORE, must be removed!
             */
            Conversation conv = conversationRepository.getById(conversationId);
            messageAddedEventProcessor.publishMessageAddedEvent(conv, messageId.toString(), text, postBoxRepository.getUserUnreadCounts(userId));
        }
    }


    private List<Participant> getParticipants(Conversation rtsConversation) {
        List<Participant> participants = new ArrayList<>();
        participants.add(new Participant(getBuyerUserId(rtsConversation), customValue(rtsConversation, "buyer-name"), rtsConversation.getBuyerId(), BUYER));
        participants.add(new Participant(getSellerUserId(rtsConversation), customValue(rtsConversation, "seller-name"), rtsConversation.getSellerId(), SELLER));
        return participants;
    }

    private String getMessageSenderUserId(Conversation rtsConversation, com.ecg.replyts.core.api.model.conversation.Message rtsMessage) {
        return rtsMessage.getMessageDirection() == BUYER_TO_SELLER ? getBuyerUserId(rtsConversation) : getSellerUserId(rtsConversation);
    }

    /**
     * Retrieve {@code Buyer-Id} using specific implementation of {@link UserIdentifierService}.
     *
     * @param conversation conversation created from events.
     * @return {@code buyer-id} retrieved from {@link UserIdentifierService} or {@code null} if identifier service cannot find buyer-id.
     */
    @Nullable
    private String getBuyerUserId(Conversation conversation) {
        return userIdentifierService.getBuyerUserId(conversation).orElse(null);
    }

    /**
     * Retrieve {@code Seller-Id} using specific implementation of {@link UserIdentifierService}.
     *
     * @param conversation conversation created from events.
     * @return {@code seller-id} retrieved from {@link UserIdentifierService} or {@code null} if identifier service cannot find seller-id.
     */
    @Nullable
    private String getSellerUserId(Conversation conversation) {
        return userIdentifierService.getSellerUserId(conversation).orElse(null);
    }

    private String customValue(Conversation rtsConversation, String customValueKey) {
        return rtsConversation.getCustomValues().get(customValueKey);
    }

    private MessageDirection getMessageDirection(Participant buyer, Participant seller, String senderId) {
        if (Objects.equals(buyer.getUserId(), senderId)) {
            return BUYER_TO_SELLER;
        } else if (Objects.equals(seller.getUserId(), senderId)) {
            return MessageDirection.SELLER_TO_BUYER;
        }

        throw new IllegalArgumentException("Sender User ID does not fit to any participant.");
    }
}