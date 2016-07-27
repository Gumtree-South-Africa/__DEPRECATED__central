package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
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
                        Optional.ofNullable(Iterables.getLast(conversation.getMessages()).getMessageDirection().name()),
                        customValueAsLong(conversation, "user-id-buyer"),
                        customValueAsLong(conversation, "user-id-seller"))
        );

        // we don't want to create zero length post-boxes
        if (!finalThreads.isEmpty()) {
            PostBox postBoxToWrite = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), finalThreads, maxAgeDays);
            postBoxRepository.write(postBoxToWrite);
            postBoxWriteCallback.success(postBoxToWrite, newReplyArrived);
        }
    }

    private Optional<Long> customValueAsLong(Conversation conversation, String customValueKey) {
        if (conversation.getCustomValues().get(customValueKey) == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(conversation.getCustomValues().get(customValueKey)));
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        Optional<MessageResponse> latestMessage = messageResponseFactory.latestMessage(email, conversation);
        if (latestMessage.isPresent()) {
            return Optional.of(MessageCenterUtils.truncateText(latestMessage.get().getTextShortTrimmed(), 250));
        }
        return Optional.empty();
    }

    public interface PostBoxWriteCallback {
        void success(PostBox numUnread, boolean markedAsUnread);
    }
}
