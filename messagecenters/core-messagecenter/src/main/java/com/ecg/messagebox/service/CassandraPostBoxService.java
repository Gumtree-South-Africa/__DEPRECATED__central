package com.ecg.messagebox.service;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.resources.requests.PartnerMessagePayload;
import com.ecg.messagebox.events.MessageAddedEventProcessor;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static java.util.Arrays.asList;
import static org.joda.time.DateTime.now;

/**
 * TODO: PB: Remove one line methods and remove spaghetti code. Remove dependency on CORE
 * After MessageBox upgrade is done.
 */
@Component
public class CassandraPostBoxService implements PostBoxService {
    private static final Counter COUNTER_MARK_AS_READ = TimingReports.newCounter("message-box.mark-as-read");
    private static final Counter COUNTER_MARK_AS_READ_EVENTS_EMITTED = TimingReports.newCounter("message-box.mark-as-read-events");

    private static final Logger LOG = LoggerFactory.getLogger(CassandraPostBoxService.class);

    public static final String SKIP_RESPONSE_DATA_HEADER = "X-Skip-Response-Data";

    private final CassandraPostBoxRepository postBoxRepository;
    private final UserIdentifierService userIdentifierService;
    private final ResponseDataCalculator responseDataCalculator;
    private final MessageAddedEventProcessor messageAddedEventProcessor;
    private final ConversationRepository conversationRepository;
    private final ConversationEventService conversationEventService;

    @Value("${replyts.tenant.short}")
    private String shortTenant;

    @Value("${messagebox.emit.markasread.unconditionally:false}")
    private boolean emitMarkAsReadUnconditionally = false;

    static final String SYSTEM_MESSAGE_USER_ID = "-1";

    @Autowired
    public CassandraPostBoxService(
            CassandraPostBoxRepository postBoxRepository,
            UserIdentifierService userIdentifierService,
            ResponseDataCalculator responseDataCalculator,
            MessageAddedEventProcessor messageAddedEventProcessor,
            ConversationRepository conversationRepository,
            ConversationEventService conversationEventService) {

        this.postBoxRepository = postBoxRepository;
        this.userIdentifierService = userIdentifierService;
        this.responseDataCalculator = responseDataCalculator;
        this.messageAddedEventProcessor = messageAddedEventProcessor;
        this.conversationRepository = conversationRepository;
        this.conversationEventService = conversationEventService;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void processNewMessage(String userId,
                                  com.ecg.replyts.core.api.model.conversation.Conversation rtsConversation,
                                  com.ecg.replyts.core.api.model.conversation.Message rtsMessage,
                                  boolean isNewReply,
                                  String cleanMessageText) {

        String senderUserId = getMessageSenderUserId(rtsConversation, rtsMessage);

        String messageTypeStr = rtsMessage.getCaseInsensitiveHeaders().getOrDefault("X-Message-Type", MessageType.EMAIL.getValue()).toLowerCase();
        MessageType messageType = MessageType.get(messageTypeStr);
        String customData = rtsMessage.getCaseInsensitiveHeaders().get("X-Message-Metadata");
        UUID messageId = UUID.fromString(rtsMessage.getId());
        List<Attachment> attachments = rtsMessage.getAttachmentFilenames()
                .stream()
                .map(fileName -> ImmutableAttachment.builder()
                        .fileName(fileName)
                        .messageID(rtsMessage.getId())
                        .build())
                .collect(Collectors.toList());
        Message newMessage = new Message(messageId, cleanMessageText, senderUserId, messageType, customData, rtsMessage.getCaseInsensitiveHeaders(), attachments);
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
                            rtsConversation.getMessages().get(0).getCaseInsensitiveHeaders().get("Subject"),
                            rtsMessage.getCaseInsensitiveHeaders().get("X-Conversation-Title"),
                            rtsMessage.getCaseInsensitiveHeaders().get("X-Conversation-Image-Url")
                    )
            );

            postBoxRepository.createConversation(userId, newConversation, newMessage, isNewReply);
        }

        if (allowReponseDataComputation(rtsMessage)) {
            responseDataCalculator.storeResponseData(userId, rtsConversation, rtsMessage);
        } else {
            LOG.debug("ResponseData calculation was skipped: conversation-id: {}, message-id: {}", rtsConversation.getId(), messageId);
        }
    }

    private static boolean allowReponseDataComputation(com.ecg.replyts.core.api.model.conversation.Message message) {
        String skipResponseData = message.getCaseInsensitiveHeaders().get(SKIP_RESPONSE_DATA_HEADER);
        return !Boolean.parseBoolean(skipResponseData);
    }

    @Override
    public Optional<ConversationThread> getConversation(String userId, String conversationId, String messageIdCursor, int messagesLimit) {
        return postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursor, messagesLimit);
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId,
                                                               String messageIdCursor, int messagesLimit) throws InterruptedException {
        COUNTER_MARK_AS_READ.inc();
        Optional<ConversationThread> maybeConversation =
                postBoxRepository.getConversationWithMessages(userId, conversationId, messageIdCursor, messagesLimit);
        if (!maybeConversation.isPresent()) {
            return Optional.empty();
        }
        ConversationThread conversation = maybeConversation.get();
        List<Participant> participants = conversation.getParticipants();
        String otherParticipantUserId = participants.get(0).getUserId().equals(userId) ? participants.get(1).getUserId()
                : participants.get(0).getUserId();
        if (emitMarkAsReadUnconditionally || conversation.getNumUnreadMessages(userId) > 0) {
            conversationEventService.sendConversationReadEvent(shortTenant, conversationId, userId);
            COUNTER_MARK_AS_READ_EVENTS_EMITTED.inc();
        }
        postBoxRepository.resetConversationUnreadCount(userId, otherParticipantUserId, conversationId, conversation.getAdId());
        return Optional.of(new ConversationThread(conversation).addNumUnreadMessages(userId, 0));
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
    public PostBox archiveConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit) throws InterruptedException {
        Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);
        for (String conversationId : conversationAdIdsMap.keySet()) {
            conversationEventService.sendConversationArchived(shortTenant, conversationId, userId);
        }
        postBoxRepository.archiveConversations(userId, conversationAdIdsMap);
        return postBoxRepository.getPostBox(userId, Visibility.ACTIVE, conversationsOffset, conversationsLimit).removeConversations(conversationIds);
    }

    @Override
    public PostBox activateConversations(String userId, List<String> conversationIds, int conversationsOffset, int conversationsLimit) throws InterruptedException {
        Map<String, String> conversationAdIdsMap = postBoxRepository.getConversationAdIdsMap(userId, conversationIds);
        for (String conversationId : conversationAdIdsMap.keySet()) {
            conversationEventService.sendConversationActivated(shortTenant, conversationId, userId);
        }
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

    public List<Participant> getParticipants(Conversation rtsConversation) {
        return asList(new Participant(getBuyerUserId(rtsConversation), customValue(rtsConversation, "buyer-name"), rtsConversation.getBuyerId(), BUYER),
                new Participant(getSellerUserId(rtsConversation), customValue(rtsConversation, "seller-name"), rtsConversation.getSellerId(), SELLER));
    }

    private String getMessageSenderUserId(Conversation rtsConversation, com.ecg.replyts.core.api.model.conversation.Message rtsMessage) {
        return rtsMessage.getMessageDirection() == BUYER_TO_SELLER
                ? getBuyerUserId(rtsConversation)
                : getSellerUserId(rtsConversation);
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