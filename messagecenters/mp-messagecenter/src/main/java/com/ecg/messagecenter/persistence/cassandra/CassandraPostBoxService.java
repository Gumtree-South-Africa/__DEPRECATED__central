package com.ecg.messagecenter.persistence.cassandra;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.*;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.TimingReports.newHistogram;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static org.joda.time.DateTime.now;

@Component("oldCassandraPostBoxService")
public class CassandraPostBoxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPostBoxService.class);

    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = newHistogram("webapi-postbox-num-messages-of-conversation");
    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = newHistogram("webapi-postbox-num-conversations-of-postbox");
    private static final String X_MESSAGE_TYPE = "X-Message-Type";

    private final CassandraPostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;
    private final PostBoxResponseBuilder postBoxResponseBuilder;
    private final int previewLastMessageMaxChars;

    private final Timer processNewMessageTimer = newTimer("postBoxService.processNewMessage");
    private final Timer getConversationTimer = newTimer("postBoxService.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxService.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxService.getConversations");
    private final Timer deleteConversationsTimer = newTimer("postBoxService.deleteConversations");
    private final Timer getUnreadCountsTimer = newTimer("postBoxService.getUnreadCounts");
    private final Timer getResponseDataTimer = newTimer("postBoxService.getResponseData");

    @Autowired
    public CassandraPostBoxService(CassandraPostBoxRepository postBoxRepository,
                                   ConversationRepository conversationRepository,
                                   UserIdentifierService userIdentifierService,
                                   @Value("${replyts.maxPreviewMessageCharacters:250}") int previewLastMessageMaxChars) {
        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.postBoxResponseBuilder = new PostBoxResponseBuilder(conversationRepository, userIdentifierService);
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
        this.previewLastMessageMaxChars = previewLastMessageMaxChars;
    }

    @Override
    public void processNewMessage(String postBoxId, Conversation conversation, Message newMessage, ConversationRole conversationRole,
                                  boolean newReplyArrived) {

        try (Timer.Context ignored = processNewMessageTimer.time()) {

            // we don't want to update postbox for users if conversation was closed
            if (ConversationState.CLOSED == conversation.getState()) {
                return;
            }

            Optional<DateTime> lastMessageCreatedAt = newMessage.getReceivedAt() != null ? Optional.of(newMessage.getReceivedAt())
                    : Optional.of(conversation.getLastModifiedAt());

            ConversationThread newConversationThread = new ConversationThread(
                    conversation.getAdId(),
                    conversation.getId(),
                    conversation.getCreatedAt(),
                    now(),
                    conversation.getLastModifiedAt(),
                    // In Cassandra the message unread counter is stored in a separate table. Though we won't use the
                    // field here, we keep it to stay compatible with the Riak implementation.
                    // Once we no longer need the Riak implementation, this field can be removed.
                    0,
                    extractPreviewLastMessage(conversation, newMessage),
                    customValue(conversation, "buyer-name"),
                    customValue(conversation, "seller-name"),
                    Optional.ofNullable(conversation.getBuyerId()),
                    Optional.ofNullable(newMessage.getMessageDirection().name()),
                    customValueAsLong(conversation, userIdentifierService.getBuyerUserIdName()),
                    customValueAsLong(conversation, userIdentifierService.getSellerUserIdName()),
                    lastMessageCreatedAt);

            postBoxRepository.addReplaceConversationThread(postBoxId, newConversationThread);

            if (newReplyArrived) {
                postBoxRepository.incrementConversationUnreadMessagesCount(postBoxId, conversation.getId());
            }

            handleResponseData(postBoxId, conversation, newMessage, conversationRole);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                return Optional.empty();
            }

            int unreadMessagesCount = postBoxRepository.getConversationUnreadMessagesCount(postBoxId, conversationId);

            return createConversationResponse(conversation, unreadMessagesCount, postBoxId);
        }
    }

    @Override
    public Optional<ConversationResponse> markConversationAsRead(String postBoxId, String conversationId) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                return Optional.empty();
            }

            postBoxRepository.resetConversationUnreadMessagesCountAsync(postBoxId, conversationId);

            return createConversationResponse(conversation, 0, postBoxId);
        }
    }

    @Override
    public PostBoxResponse getConversations(String postBoxId, Integer size, Integer page) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            PostBox postBox = postBoxRepository.getPostBox(postBoxId);

            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

            return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
        }
    }

    @Override
    public PostBoxResponse deleteConversations(String postBoxId, List<String> conversationIds, Integer size, Integer page) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            postBoxRepository.deleteConversationThreadsAsync(postBoxId, conversationIds);

            // performance optimization, in case the client is not interested in the new postbox
            if (size == 0) return null;

            PostBox postBox = postBoxRepository.getPostBox(postBoxId);

            // Delete is asynchronous, we probably will not see the change yet. This makes sure we don't return the just deleted conversations:
            postBox.removeConversations(conversationIds);

            return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
        }
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {

            return postBoxRepository.getUnreadCounts(postBoxId);
        }
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {

            return postBoxRepository.getResponseData(userId);
        }
    }

    private Optional<ConversationResponse> createConversationResponse(Conversation conversation, int unreadMessagesCount, String postBoxId) {
        Optional<ConversationResponse> responseOptional = ConversationResponse.create(unreadMessagesCount, postBoxId, conversation, userIdentifierService);
        if (responseOptional.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(responseOptional.get().getMessages().size());
            return responseOptional;
        } else {
            LOGGER.info("Postbox {}, conversation {} contains no visible messages", postBoxId, conversation.getId());
            return Optional.empty();
        }
    }

    private Optional<String> customValue(Conversation conversation, String customValueKey) {
        return Optional.ofNullable(conversation.getCustomValues().get(customValueKey));
    }

    private Optional<Long> customValueAsLong(Conversation conversation, String customValueKey) {
        return customValue(conversation, customValueKey).map(Long::valueOf);
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, Message message) {
        String filteredMessage = messageResponseFactory.getCleanedMessage(conversation, message);
        return Optional.of(MessageCenterUtils.truncateText(filteredMessage, previewLastMessageMaxChars));
    }

    private void handleResponseData(String postBoxId, Conversation conversation, Message newMessage, ConversationRole conversationRole) {
        // BR: only for sellers
        if (ConversationRole.Seller == conversationRole) {
            // BR: only for conversations initiated by buyer
            if (conversation.getMessages().size() == 1 && MessageDirection.BUYER_TO_SELLER == newMessage.getMessageDirection()) {
                ResponseData initialResponseData = new ResponseData(postBoxId, conversation.getId(), conversation.getCreatedAt(),
                        MessageType.get(newMessage.getHeaders().get(X_MESSAGE_TYPE)));
                postBoxRepository.addOrUpdateResponseDataAsync(initialResponseData);
            } else if (conversation.getMessages().size() > 1 && MessageDirection.BUYER_TO_SELLER == conversation.getMessages().get(0).getMessageDirection()) {
                // BR: only consider the first response from seller
                java.util.Optional<Message> firstSellerToBuyerMessage = conversation.getMessages().stream()
                        .filter(message -> MessageDirection.SELLER_TO_BUYER == message.getMessageDirection()).findFirst();
                if (firstSellerToBuyerMessage.isPresent() && firstSellerToBuyerMessage.get().getId().equals(newMessage.getId())) {
                    int responseSpeed = Minutes.minutesBetween(conversation.getCreatedAt(), newMessage.getReceivedAt()).getMinutes();
                    // Only the response speed value is different from the initially created response data. The conversation type is the type of the first message.
                    ResponseData updatedResponseData = new ResponseData(postBoxId, conversation.getId(), conversation.getCreatedAt(),
                            MessageType.get(conversation.getMessages().get(0).getHeaders().get(X_MESSAGE_TYPE)), responseSpeed);
                    postBoxRepository.addOrUpdateResponseDataAsync(updatedResponseData);
                }
            }
        }
    }
}