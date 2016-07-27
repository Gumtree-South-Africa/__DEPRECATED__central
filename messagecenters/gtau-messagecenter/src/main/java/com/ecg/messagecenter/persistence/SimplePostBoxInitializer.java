package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessageType;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Component
public class SimplePostBoxInitializer {
    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Value("${replyts.maxPreviewMessageCharacters:250}")
    private int maxChars;

    @Value("${replyts.maxConversationAgeDays}")
    private int maxAgeDays;

    private MessagesResponseFactory messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());

    public void moveConversationToPostBox(
            String email,
            Conversation conversation,
            boolean newReplyArrived,
            PostBoxWriteCallback postBoxWriteCallback) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(ConversationRole.getRole(email, conversation))) {
            return;
        }

        PostBox<ConversationThread> postBox = postBoxRepository.byId(email);

        List<ConversationThread> finalThreads = Lists.newArrayList();
        for (ConversationThread thread : postBox.getConversationThreads()) {
            if (!thread.getConversationId().equals(conversation.getId())) {
                finalThreads.add(thread);
            }
        }

        if (newReplyArrived) {
            postBox.incNewReplies();
        }

        Message lastMessage = Iterables.getLast(conversation.getMessages());

        finalThreads.add(
                new ConversationThread(
                        conversation.getAdId(),
                        conversation.getId(),
                        conversation.getCreatedAt(),
                        now(),
                        conversation.getLastModifiedAt(),
                        newReplyArrived,
                        extractPreviewLastMessage(conversation, email),
                        Optional.ofNullable(conversation.getCustomValues().get("buyer-name")),
                        Optional.ofNullable(conversation.getCustomValues().get("seller-name")),
                        Optional.ofNullable(conversation.getBuyerId()),
                        Optional.ofNullable(lastMessage.getMessageDirection().name()),
                        Optional.ofNullable(MessageType.getRobot(lastMessage)),
                        Optional.ofNullable(MessageType.getOffer(lastMessage)))
        );

        // we don't want to create zero length post-boxes
        if (finalThreads.size() > 0) {
            PostBox postBoxToWrite = new PostBox(email, Optional.<Long>of(postBox.getNewRepliesCounter().getValue()), finalThreads, maxAgeDays);
            postBoxRepository.write(postBoxToWrite);
            postBoxWriteCallback.success(postBoxToWrite, newReplyArrived);
        }
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        Optional<MessageResponse> latestMessage = messageResponseFactory.latestMessage(email, conversation);
        if (latestMessage.isPresent()) {
            return Optional.of(MessageCenterUtils.truncateText(latestMessage.get().getTextShortTrimmed(), maxChars));
        }
        return Optional.empty();
    }

    public interface PostBoxWriteCallback {
        void success(PostBox numUnread, boolean markedAsUnread);
    }
}