package com.ecg.messagecenter.persistence.riak;

import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.*;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.PostBoxResponseBuilder;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.persistence.ResponseData;
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

public class RiakPostBoxService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakPostBoxService.class);

    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");
    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final RiakPostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;
    private final PostBoxResponseBuilder postBoxResponseBuilder;
    private final int previewLastMessageMaxChars;

    @Autowired
    public RiakPostBoxService(RiakPostBoxRepository postBoxRepository,
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
                                  Message message,
                                  ConversationRole conversationRole,
                                  boolean newReplyArrived,
                                  Optional<NewMessageListener> newMessageListener) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(conversationRole)) {
            return;
        }

        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        int previousUnreadCounter = 0;
        List<ConversationThread> finalThreads = Lists.newArrayList();
        for (ConversationThread thread : postBox.getConversationThreads()) {
            if (thread.getConversationId().equals(conversation.getId())) {
                // Skip it but take the counter
                previousUnreadCounter = thread.getNumUnreadMessages();
            } else {
                finalThreads.add(thread);
            }
        }

        List<Message> messages = messageResponseFactory.filterMessages(conversation.getMessages(), conversation, postBoxId);
        Message lastMessage = messages.size() > 0 ? Iterables.getLast(messages) : null;

        Optional<MessageResponse> messageResponseOptional = getMessageResponseOptional(conversation, postBoxId);

        finalThreads.add(
                new ConversationThread(
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

        PostBox postBoxToWrite = new PostBox(postBoxId, finalThreads);
        postBoxRepository.write(postBoxToWrite);

        if (newMessageListener.isPresent()) {
            newMessageListener.get().success(postBoxId, postBoxToWrite.getNewRepliesCounter(), newReplyArrived);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String postBoxId, String conversationId) {
        Optional<ConversationThread> ctOptional = postBoxRepository.getConversationThread(postBoxId, conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
                return absent();
            }

            long unreadMessagesCount = ctOptional.get().getNumUnreadMessages();
            Optional<ConversationResponse> responseOptional = ConversationResponse.create(unreadMessagesCount, postBoxId, conversation, userIdentifierService);
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
    public Optional<ConversationResponse> markConversationAsRead(String postBoxId, String conversationId) {
        PostBox postBox = postBoxRepository.getPostBox(postBoxId);
        Optional<ConversationThread> ctOptional = postBox.lookupConversation(conversationId);

        if (ctOptional.isPresent()) {
            Conversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id {} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
                return absent();
            }

            if (ctOptional.get().isContainsUnreadMessages()) {
                markConversationAsRead(postBoxId, conversationId, postBox);
            }

            Optional<ConversationResponse> responseOptional = ConversationResponse.create(0, postBoxId, conversation, userIdentifierService);
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
    public PostBoxResponse markConversationsAsRead(String postBoxId, Integer size, Integer page) {
        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

        if (postBox.getNumUnreadConversations() > 0) {
            postBox = postBox.markAllAsRead();
            postBoxRepository.write(postBox);
        }

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
    }

    @Override
    public PostBoxResponse deleteConversations(String postBoxId, List<String> conversationIds, Integer page, Integer size) {
        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        conversationIds.forEach(postBox::removeConversation);

        postBoxRepository.write(postBox);

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        return postBoxRepository.getUnreadCounts(postBoxId);
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        throw new UnsupportedOperationException("Response data is not implemented for Riak");
    }

    @Override
    public PostBoxResponse getConversations(String postBoxId, Integer size, Integer page) {
        PostBox postBox = postBoxRepository.getPostBox(postBoxId);

        API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

        return postBoxResponseBuilder.buildPostBoxResponse(postBoxId, size, page, postBox);
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