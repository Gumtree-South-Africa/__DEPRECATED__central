package com.ecg.messagecenter.persistence.cassandra;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.NewMessageListener;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static com.google.common.base.Optional.absent;
import static org.joda.time.DateTime.now;

public class CassandraPostBoxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPostBoxService.class);

    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");
    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final CassandraPostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;
    private final PostBoxResponseBuilder postBoxResponseBuilder;
    private final int previewLastMessageMaxChars;

    @Autowired
    public CassandraPostBoxService(CassandraPostBoxRepository postBoxRepository,
                                   ConversationRepository conversationRepository,
                                   UserIdentifierService userIdentifierService,
                                   @Value("${replyts.maxPreviewMessageCharacters:250}") int previewLastMessageMaxChars) {
        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
        this.previewLastMessageMaxChars = previewLastMessageMaxChars;
        this.postBoxResponseBuilder = new PostBoxResponseBuilder(conversationRepository, userIdentifierService);
    }

    @Override
    public void processNewMessage(String postBoxId,
                                  Conversation conversation,
                                  Message newMessage,
                                  ConversationRole conversationRole,
                                  boolean newReplyArrived,
                                  Optional<NewMessageListener> newMessageListener) {

        // we don't want to update postbox for users if conversation was closed
        if (ConversationState.CLOSED == conversation.getState()) {
            return;
        }

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
                Optional.fromNullable(conversation.getBuyerId()),
                Optional.fromNullable(newMessage.getMessageDirection().name()),
                customValueAsLong(conversation, "negotiationid"),
                customValueAsLong(conversation, userIdentifierService.getBuyerUserIdName()),
                customValueAsLong(conversation, userIdentifierService.getSellerUserIdName()),
                Optional.fromNullable(newMessage.getReceivedAt()).or(Optional.of(conversation.getLastModifiedAt())));

        postBoxRepository.addReplaceConversationThread(postBoxId, newConversationThread);

        if (newReplyArrived) {
            postBoxRepository.incrementConversationUnreadMessagesCount(postBoxId, conversation.getId());
        }

        if (newMessageListener.isPresent()) {
            long postBoxUnreadMessagesCount = postBoxRepository.getUnreadCounts(postBoxId).getNumUnreadMessages();
            newMessageListener.get().success(postBoxId, postBoxUnreadMessagesCount, newReplyArrived);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String postBoxId, String conversationId) {
        Conversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            return absent();
        }

        int unreadMessagesCount = postBoxRepository.getConversationUnreadMessagesCount(postBoxId, conversationId);

        return createConversationResponse(conversation, unreadMessagesCount, postBoxId);
    }

    @Override
    public Optional<ConversationResponse> markConversationAsRead(String postBoxId, String conversationId) {
        Conversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            return absent();
        }

        postBoxRepository.resetConversationUnreadMessagesCountAsync(postBoxId, conversationId);

        return createConversationResponse(conversation, 0, postBoxId);
    }

    private Optional<ConversationResponse> createConversationResponse(Conversation conversation, int unreadMessagesCount, String postBoxId) {
        Optional<ConversationResponse> responseOptional = ConversationResponse.create(unreadMessagesCount, postBoxId, conversation, userIdentifierService);
        if (responseOptional.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(responseOptional.get().getMessages().size());
            return responseOptional;
        } else {
            LOGGER.info("Postbox {}, conversation {} contains no visible messages", postBoxId, conversation.getId());
            return absent();
        }
    }

    @Override
    public PostBoxResponse markConversationsAsRead(String postBoxId, Integer size, Integer page) {
        List<String> conversationThreadIds = postBoxRepository.getConversationThreadIds(postBoxId);
        conversationThreadIds.forEach(conversationId -> postBoxRepository.resetConversationUnreadMessagesCountAsync(postBoxId, conversationId));

        // performance optimization, in case the client is not interested in the new postbox
        if (size == 0) return null;

        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

        // Reset is asynchronous, we probably will not see the change yet. This makes sure we don't return unread conversations:
        PostBox newPostBox = postBox.markAllAsRead();

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, newPostBox);
    }

    @Override
    public PostBoxResponse getConversations(String postBoxId, Integer size, Integer page) {
        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
    }

    @Override
    public PostBoxResponse deleteConversations(String postBoxId, List<String> conversationIds, Integer page, Integer size) {
        postBoxRepository.deleteConversationThreadsAsync(postBoxId, conversationIds);

        // performance optimization, in case the client is not interested in the new postbox
        if (size == 0) return null;

        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        // Delete is asynchronous, we probably will not see the change yet. This makes sure we don't return the just deleted conversations:
        postBox.removeConversations(conversationIds);

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        return postBoxRepository.getUnreadCounts(postBoxId);
    }

    private Optional<String> customValue(Conversation conversation, String customValueKey) {
        return Optional.fromNullable(conversation.getCustomValues().get(customValueKey));
    }

    private Optional<Long> customValueAsLong(Conversation conversation, String customValueKey) {
        return customValue(conversation, customValueKey).transform(Long::valueOf);
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, Message message) {
        String filteredMessage = messageResponseFactory.getCleanedMessage(conversation, message);
        return Optional.of(MessageCenterUtils.truncateText(filteredMessage, previewLastMessageMaxChars));
    }
}