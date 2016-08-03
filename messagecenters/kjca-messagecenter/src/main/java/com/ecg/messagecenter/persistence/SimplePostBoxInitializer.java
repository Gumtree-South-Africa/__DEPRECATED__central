package com.ecg.messagecenter.persistence;

import ca.kijiji.replyts.TextAnonymizer;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.collect.Iterables;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.joda.time.DateTimeZone.UTC;

@Component
public class SimplePostBoxInitializer {
    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private ConversationBlockRepository conversationBlockRepository;

    @Autowired
    private TextAnonymizer textAnonymizer;

    @Value("${replyts.maxPreviewMessageCharacters:250}")
    private int maxChars;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    private MessagesResponseFactory messageResponseFactory;

    @PostConstruct
    private void createMessagesResponseFactory() {
        messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer(), textAnonymizer);
    }

    public void moveConversationToPostBox(
            String email,
            Conversation conversation,
            boolean newReplyArrived,
            PostBoxWriteCallback postBoxWriteCallback
    ) {

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(ConversationRole.getRole(email, conversation))) {
            return;
        }

        DateTime now = DateTime.now(UTC);
        PostBox<ConversationThread> postBox = postBoxRepository.byId(email);

        Optional<String> previewLastMessage = extractPreviewLastMessage(conversation, email);
        Optional<ConversationThread> existingThread = postBox.lookupConversation(conversation.getId());

        if (!previewLastMessage.isPresent() || shouldReuseExistingThread(existingThread, previewLastMessage.get())) {
            // Don't display empty conversations in MB, and don't add empty messages to existing convos
            return;
        }

        if (recipientBlockedSender(email, conversation)) {
            return;
        }

        List<ConversationThread> finalThreads = postBox.getConversationThreads()
                .stream()
                .filter(thread -> !thread.getConversationId().equals(conversation.getId()))
                .collect(Collectors.toList());

        finalThreads.add(new ConversationThread(
                conversation.getAdId(),
                conversation.getId(),
                conversation.getCreatedAt(),
                now,
                conversation.getLastModifiedAt(),
                newReplyArrived,
                previewLastMessage,
                Optional.ofNullable(conversation.getCustomValues().get("buyer-name")),
                Optional.ofNullable(conversation.getCustomValues().get("seller-name")),
                Optional.ofNullable(conversation.getBuyerId()),
                Optional.ofNullable(Iterables.getLast(conversation.getMessages()).getMessageDirection().name())));

        if (newReplyArrived) {
            postBox.incNewReplies();
        }

        PostBox postBoxToWrite = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), finalThreads, maxConversationAgeDays);
        postBoxRepository.write(postBoxToWrite);
        postBoxWriteCallback.success(postBoxToWrite, newReplyArrived);
    }

    private boolean recipientBlockedSender(String email, Conversation conversation) {
        ConversationBlock conversationBlock = conversationBlockRepository.byConversationId(conversation.getId());
        if (conversationBlock == null) {
            return false;
        }

        if (email.equals(conversation.getBuyerId()) && conversationBlock.getBuyerBlockedSellerAt().isPresent()) {
            return true;
        }

        if (email.equals(conversation.getSellerId()) && conversationBlock.getSellerBlockedBuyerAt().isPresent()) {
            return true;
        }

        return false;
    }

    private boolean shouldReuseExistingThread(Optional<ConversationThread> existingThread, @Nonnull String newMessage) {
        if (!existingThread.isPresent()) { // nothing to reuse
            return false;
        }

        Optional<String> lastMessage = existingThread.flatMap(ConversationThread::getPreviewLastMessage);
        return lastMessage.isPresent() && lastMessage.get().equals(newMessage);
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        Optional<MessageResponse> latestMessage = messageResponseFactory.latestMessage(email, conversation);
        if (latestMessage.isPresent()) {
            final String truncatedMessage = MessageCenterUtils.truncateText(latestMessage.get().getTextShortTrimmed(), maxChars);
            return Optional.of(textAnonymizer.anonymizeText(conversation, truncatedMessage));
        }
        return Optional.empty();
    }

    public interface PostBoxWriteCallback {

        void success(PostBox numUnread, boolean markedAsUnread);

    }
}
