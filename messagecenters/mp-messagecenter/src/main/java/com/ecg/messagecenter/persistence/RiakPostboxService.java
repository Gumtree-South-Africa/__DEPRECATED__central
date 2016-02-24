package com.ecg.messagecenter.persistence;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Optional.absent;
import static org.joda.time.DateTime.now;

public class RiakPostboxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakPostboxService.class);

    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    private final PostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;
    private final int previewLastMessageMaxChars;

    @Autowired
    public RiakPostboxService(PostBoxRepository postBoxRepository,
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
                                  Message message,
                                  ConversationRole conversationRole,
                                  boolean newReplyArrived,
                                  Optional<PushMessageOnUnreadConversationCallback> postBoxWriteCallback) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(conversationRole)) {
            return;
        }

        PostBox postBox = postBoxRepository.byId(userId);

        long previousUnreadCounter = 0;
        List<ConversationThread> finalThreads = Lists.newArrayList();
        for (ConversationThread thread : postBox.getConversationThreads()) {
            if (thread.getConversationId().equals(conversation.getId())) {
                // Skip it but take the counter
                previousUnreadCounter = thread.getNumUnreadMessages();
            } else {
                finalThreads.add(thread);
            }
        }

        List<Message> messages = messageResponseFactory.filterMessages(conversation.getMessages(), conversation, userId);
        Message lastMessage = messages.size() > 0 ? Iterables.getLast(messages) : null;

        Optional<MessageResponse> messageResponseOptional = getMessageResponseOptional(conversation, userId);

        finalThreads.add(
                new ConversationThread(
                        true,
                        conversation.getAdId(),
                        conversation.getId(),
                        conversation.getCreatedAt(),
                        now(),
                        conversation.getLastModifiedAt(),
                        previousUnreadCounter + (newReplyArrived ? 1 : 0),
                        messageResponseOptional.transform(msg -> MessageCenterUtils.truncateText(msg.getTextShortTrimmed(), previewLastMessageMaxChars)),
                        customValue(conversation, "buyer-name"),
                        customValue(conversation, "seller-name"),
                        Optional.fromNullable(conversation.getBuyerId()),
                        (lastMessage != null) ? Optional.of(lastMessage.getMessageDirection().name()) : Optional.<String>absent(),
                        customValueAsLong(conversation, "negotiationid"),
                        customValueAsLong(conversation, userIdentifierService.getBuyerUserIdName()),
                        customValueAsLong(conversation, userIdentifierService.getSellerUserIdName()),
                        Optional.of((lastMessage != null) ? lastMessage.getReceivedAt() : conversation.getLastModifiedAt()))
        );

        PostBox postBoxToWrite = new PostBox(userId, finalThreads);
        postBoxRepository.write(postBoxToWrite);

        if (postBoxWriteCallback.isPresent()) {
            postBoxWriteCallback.get().success(userId, postBoxToWrite.getNewRepliesCounter(), newReplyArrived);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String userId, String conversationId) {
        Optional<ConversationThread> ctOptional = postBoxRepository.getConversationThread(userId, conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
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
        PostBox postBox = postBoxRepository.byId(userId);
        Optional<ConversationThread> ctOptional = postBox.lookupConversation(conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
                return absent();
            }

            if (ctOptional.get().isContainsUnreadMessages()) {
                markConversationAsRead(userId, conversationId, postBox);
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

    private void markConversationAsRead(String userId, String conversationId, PostBox postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        boolean needsUpdate = false;
        for (ConversationThread item : postBox.getConversationThreads()) {
            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                threadsToUpdate.add(item.sameButRead());
                needsUpdate = true;
            } else {
                threadsToUpdate.add(item);
            }
        }

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox postBoxToUpdate = new PostBox(userId, threadsToUpdate);
            postBoxRepository.write(postBoxToUpdate);
        }
    }

    private Optional<String> customValue(Conversation conversation, String customValueKey) {
        return Optional.fromNullable(conversation.getCustomValues().get(customValueKey));
    }

    private Optional<Long> customValueAsLong(Conversation conversation, String customValueKey) {
        return customValue(conversation, customValueKey).transform(Long::valueOf);
    }

    private Optional<MessageResponse> getMessageResponseOptional(Conversation conversation, String id) {
        return messageResponseFactory.latestMessage(id, conversation);
    }
}