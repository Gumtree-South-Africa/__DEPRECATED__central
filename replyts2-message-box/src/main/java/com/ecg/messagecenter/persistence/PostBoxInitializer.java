package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.joda.time.DateTime.now;

/**
 * User: maldana
 * Date: 30.10.13
 * Time: 15:27
 *
 * @author maldana@ebay.de
 */
public class PostBoxInitializer {

    private final PostBoxRepository postBoxRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public PostBoxInitializer(PostBoxRepository postBoxRepository,
                              UserIdentifierService userIdentifierService) {
        this.postBoxRepository = postBoxRepository;
        this.userIdentifierService = userIdentifierService;
        this.messageResponseFactory = new MessagesResponseFactory(userIdentifierService);
    }

    public void moveConversationToPostBox(
            String id,
            ConversationRole role,
            Conversation conversation,
            boolean newReplyArrived,
            Optional<PushMessageOnUnreadConversationCallback> postBoxWriteCallback) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(role)) {
            return;
        }

        PostBox postBox = postBoxRepository.byId(id);

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

        List<Message> messages = messageResponseFactory.filterMessages(conversation.getMessages(), conversation, id);
        Message lastMessage = messages.size() > 0 ? Iterables.getLast(messages) : null;

        Optional<MessageResponse> messageResponseOptional = getMessageResponseOptional(conversation, id);

        finalThreads.add(
                new ConversationThread(
                        true,
                        conversation.getAdId(),
                        conversation.getId(),
                        conversation.getCreatedAt(),
                        now(),
                        conversation.getLastModifiedAt(),
                        previousUnreadCounter + (newReplyArrived ? 1 : 0),
                        messageResponseOptional.transform(msg -> MessageCenterUtils.truncateText(msg.getTextShortTrimmed(), 250)),
                        customValue(conversation, "buyer-name"),
                        customValue(conversation, "seller-name"),
                        Optional.fromNullable(conversation.getBuyerId()),
                        (lastMessage != null) ? Optional.of(lastMessage.getMessageDirection().name()) : Optional.<String>absent(),
                        customValueAsLong(conversation, "negotiationid"),
                        customValueAsLong(conversation, userIdentifierService.getBuyerUserIdName()),
                        customValueAsLong(conversation, userIdentifierService.getSellerUserIdName()),
                        Optional.of((lastMessage != null) ? lastMessage.getReceivedAt() : conversation.getLastModifiedAt()))
        );

        PostBox postBoxToWrite = new PostBox(id, finalThreads);
        postBoxRepository.write(postBoxToWrite);

        if (postBoxWriteCallback.isPresent()) {
            postBoxWriteCallback.get().success(id, postBoxToWrite.getNewRepliesCounter(), newReplyArrived);
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

    public interface PostBoxWriteCallback {
        void success(String recipientUserId, long unreadCount, boolean markedAsUnread);
    }
}