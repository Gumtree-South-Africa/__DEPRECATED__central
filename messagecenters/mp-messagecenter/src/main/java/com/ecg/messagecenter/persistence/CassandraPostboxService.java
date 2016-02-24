package com.ecg.messagecenter.persistence;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Optional.absent;
import static org.joda.time.DateTime.now;

public class CassandraPostboxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraPostboxService.class);

    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    private final PostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;
    private final int previewLastMessageMaxChars;

    @Autowired
    public CassandraPostboxService(PostBoxRepository postBoxRepository,
                                   ConversationRepository conversationRepository,
                                   UserIdentifierService userIdentifierService,
                                   @Value("${replyts.maxPreviewMessageCharacters:250}") int previewLastMessageMaxChars) {
        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
        this.previewLastMessageMaxChars = previewLastMessageMaxChars;
    }

    @Override
    public void processNewMessage(String userId,
                                  Conversation conversation,
                                  Message newMessage,
                                  ConversationRole conversationRole,
                                  boolean newReplyArrived,
                                  Optional<PushMessageOnUnreadConversationCallback> postBoxWriteCallback) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(conversationRole)) {
            return;
        }

        ConversationThread newConversationThread = new ConversationThread(
                true,
                conversation.getAdId(),
                conversation.getId(),
                conversation.getCreatedAt(),
                now(),
                conversation.getLastModifiedAt(),
                getUnreadMessagesCount(userId, conversation.getId()) + (newReplyArrived ? 1 : 0),
                extractPreviewLastMessage(conversation, newMessage),
                customValue(conversation, "buyer-name"),
                customValue(conversation, "seller-name"),
                Optional.fromNullable(conversation.getBuyerId()),
                Optional.fromNullable(newMessage.getMessageDirection().name()),
                customValueAsLong(conversation, "negotiationid"),
                customValueAsLong(conversation, userIdentifierService.getBuyerUserIdName()),
                customValueAsLong(conversation, userIdentifierService.getSellerUserIdName()),
                Optional.fromNullable(newMessage.getReceivedAt()).or(Optional.of(conversation.getLastModifiedAt())));

        postBoxRepository.addReplaceConversationThread(userId, newConversationThread);

        if (postBoxWriteCallback.isPresent()) {
            // TODO: Add total number of unread messages per postbox
            int postBoxUnreadMessagesCount = 0;
            postBoxWriteCallback.get().success(userId, postBoxUnreadMessagesCount, newReplyArrived);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String userId, String conversationId) {
        Optional<ConversationThread> ctOptional = postBoxRepository.getConversationThread(userId, conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' table but not inside 'conversations' table", conversationId);
                return absent();
            }

            long unreadMessagesCount = ctOptional.get().getNumUnreadMessages();
            Optional<ConversationResponse> responseOptional = ConversationResponse.create(unreadMessagesCount, userId, conversation, userIdentifierService);
            if (responseOptional.isPresent()) {
                API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(responseOptional.get().getMessages().size());
                return responseOptional;
            } else {
                LOGGER.info("Conversation id {} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
                return absent();
            }
        } else {
            return absent();
        }
    }

    @Override
    public Optional<ConversationResponse> updateConversationToRead(String userId, String conversationId) {
        Optional<ConversationThread> ctOptional = postBoxRepository.getConversationThread(userId, conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' table but not inside 'conversations' table", conversationId);
                return absent();
            }

            if (ctOptional.get().isContainsUnreadMessages()) {
                postBoxRepository.addReplaceConversationThread(userId, ctOptional.get().sameButRead());
            }

            Optional<ConversationResponse> responseOptional = ConversationResponse.create(0, userId, conversation, userIdentifierService);
            if (responseOptional.isPresent()) {
                API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(responseOptional.get().getMessages().size());
                return responseOptional;
            } else {
                LOGGER.info("Conversation id {} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
                return absent();
            }
        } else {
            return absent();
        }
    }

    private long getUnreadMessagesCount(String userId, String conversationId) {
        Optional<ConversationThread> conversationThreadOptional = postBoxRepository.getConversationThread(userId, conversationId);
        if (conversationThreadOptional.isPresent()) {
            return conversationThreadOptional.get().getNumUnreadMessages();
        } else {
            return 0;
        }
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