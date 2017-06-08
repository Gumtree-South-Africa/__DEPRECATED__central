package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.RiakSimplePostBoxRepository;
import com.ecg.messagecenter.util.*;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
/**
 * User: maldana
 * Date: 30.10.13
 * Time: 15:27
 *
 * @author maldana@ebay.de
 */
@Component
public class PostBoxInitializer {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private final RiakSimplePostBoxRepository postBoxRepository;
    private final MessagesResponseFactory messageResponseFactory;
    private final int maxChars;
    private int maxAgeDays;

    @Autowired public PostBoxInitializer(RiakSimplePostBoxRepository postBoxRepository,
                                         @Value("${replyts.maxPreviewMessageCharacters:250}") int maxChars,
                                         @Value("${replyts.maxConversationAgeDays:180}") int maxAgeDays) {
        this.postBoxRepository = postBoxRepository;
        this.messageResponseFactory = new MessagesResponseFactory(new MessagesDiffer());
        this.maxChars = maxChars;
        this.maxAgeDays = maxAgeDays;
    }

    public void moveConversationToPostBox(String email, Conversation conversation,
                    boolean newReplyArrived, PostBoxWriteCallback postBoxWriteCallback) {

        Message lastMessage = Iterables.getLast(conversation.getMessages());

        logMessage(lastMessage);

        if (lastMessage.getState() != MessageState.SENT) {
            LOG.info("Ignoring message " + lastMessage);
            return;
        }

        // we don't want to further spam postbox for user if it was set as ignored before
        if (conversation.isClosedBy(ConversationRole.getRole(email, conversation))) {
            return;
        }

        PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        List<ConversationThread> finalThreads = Lists.newArrayList();
        for (ConversationThread thread : postBox.getConversationThreads()) {
            if (!thread.getConversationId().equals(conversation.getId())) {
                finalThreads.add(thread);
            }
        }

        if (newReplyArrived) {
            postBox.incNewReplies();
        }

        finalThreads.add(new ConversationThread(conversation.getAdId(), conversation.getId(),
                                        conversation.getCreatedAt(), now(),
                                        conversation.getLastModifiedAt(), newReplyArrived,
                                        extractPreviewLastMessage(conversation, email),
                                        Optional.ofNullable(conversation.getCustomValues()
                                                        .get("buyer-name")), Optional.ofNullable(
                conversation.getCustomValues().get("seller-name")),
                                        Optional.ofNullable(conversation.getBuyerId()),
                                        Optional.ofNullable(
                                                lastMessage.getMessageDirection().name()),
                                        Optional.ofNullable(com.ecg.messagecenter.util.MessageType.getRobot(lastMessage)),
                                        Optional.ofNullable(com.ecg.messagecenter.util.MessageType.getOffer(lastMessage))));

        // we don't want to create zero length post-boxes
        if (finalThreads.size() > 0) {
            PostBox<ConversationThread> postBoxToWrite = new PostBox<>(email, Optional.of(postBox.getNewRepliesCounter().getValue()), finalThreads, maxAgeDays);
            postBoxRepository.write(postBoxToWrite);
            postBoxWriteCallback.success(postBoxToWrite, newReplyArrived);
        }
    }

    private void logMessage(Message message) {
        try {
            LOG.debug("Message: " + new ObjectMapper().writeValueAsString(message));
        } catch (Exception e) {
            LOG.info(e.getMessage(), e);
        }
    }

    private Optional<String> extractPreviewLastMessage(Conversation conversation, String email) {
        Optional<MessageResponse> latestMessage =
                        messageResponseFactory.latestMessage(email, conversation);
        if (latestMessage.isPresent()) {
            return Optional.of(MessageCenterUtils
                            .truncateText(latestMessage.get().getTextShortTrimmed(), maxChars));
        }
        return Optional.empty();
    }

    public interface PostBoxWriteCallback {
        void success(PostBox<ConversationThread> numUnread, boolean markedAsUnread);
    }
}
