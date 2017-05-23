package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.de.ebayk.messagecenter.util.MessagesDiffer;
import com.ecg.de.ebayk.messagecenter.util.MessagesResponseFactory;
import com.ecg.de.ebayk.messagecenter.webapi.responses.MessageResponse;
import com.ecg.gumtree.replyts2.common.message.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.NoSuchElementException;

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
    private final int maxChars;
    private int maxConversationAgeDays;

    @Autowired
    public PostBoxInitializer(PostBoxRepository postBoxRepository,
                              @Value("${replyts.maxPreviewMessageCharacters:250}") int maxChars,
                              @Value("${replyts.maxConversationAgeDays:180}") int maxConversationAgeDays) {
        this.postBoxRepository = postBoxRepository;
        this.messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());
        this.maxChars = maxChars;
        this.maxConversationAgeDays = maxConversationAgeDays;
    }

    public void moveConversationToPostBox(
            String email,
            Conversation conversation,
            boolean newReplyArrived,
            PostBoxWriteCallback postBoxWriteCallback) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(ConversationRole.getRole(email, conversation))) {
            return;
        }

        PostBox postBox = postBoxRepository.byId(email);

        List<ConversationThread> finalThreads = Lists.newArrayList();
        for (ConversationThread thread : postBox.getConversationThreads()) {
            if (!thread.getConversationId().equals(conversation.getId())) {
                finalThreads.add(thread);
            }
        }

        if (newReplyArrived) {
            postBox.incNewReplies();
        }

        try {
            Message lastMessage = Iterables.getLast(
                    messageResponseFactory.filterMessages(conversation.getMessages(), conversation, email));

            finalThreads.add(
                    new ConversationThread(
                            conversation.getAdId(),
                            conversation.getId(),
                            conversation.getCreatedAt(),
                            now(),
                            lastMessage.getReceivedAt(),
                            newReplyArrived,
                            extractPreviewLastMessage(conversation, email),
                            Optional.fromNullable(conversation.getCustomValues().get("buyer-name")),
                            Optional.fromNullable(conversation.getCustomValues().get("seller-name")),
                            Optional.fromNullable(conversation.getBuyerId()),
                            Optional.fromNullable(conversation.getSellerId()),
                            Optional.fromNullable(lastMessage.getMessageDirection().name()))
            );
        } catch (NoSuchElementException ex) {
            // no messages in conversation, no threads to add
        }

        // we don't want to create zero length post-boxes
        if (finalThreads.size() > 0) {
            PostBox postBoxToWrite = new PostBox.PostBoxBuilder().
                    withEmail(email).
                    withNewRepliesCounter(postBox.getNewRepliesCounter().getValue()).
                    withConversationThreads(finalThreads).
                    withMaxConversationAgeDays(maxConversationAgeDays).build();
            postBoxRepository.write(postBoxToWrite);
            postBoxWriteCallback.success(postBoxToWrite, newReplyArrived);
        }
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        Optional<MessageResponse> latestMessage = messageResponseFactory.latestMessage(email, conversation);
        if (latestMessage.isPresent()) {
            return Optional.of(MessageCenterUtils.truncateText(latestMessage.get().getTextShortTrimmed(), maxChars));
        }
        return Optional.absent();
    }

    public interface PostBoxWriteCallback {

        void success(PostBox numUnread, boolean markedAsUnread);

    }


}
