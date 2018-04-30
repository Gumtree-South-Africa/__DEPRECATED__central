package com.ecg.messagecenter.ebayk.diff;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.ebayk.persistence.ConversationThread;
import com.ecg.messagecenter.ebayk.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Component
public class ConversationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationService.class);

    private SimplePostBoxRepository postBoxRepository;
    private ConversationRepository conversationRepository;

    @Autowired
    public ConversationService(SimplePostBoxRepository postBoxRepository, ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }

    public Optional<PostBoxSingleConversationThreadResponse> getConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
        }

        return createResponse(email, conversationId, postBox);
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
        }

        if (conversationThreadRequested.get().isContainsUnreadMessages()) {
            PostBox postBoxToUpdate = updatePostBox(email, conversationId, postBox);
            postBoxRepository.markConversationAsRead(postBoxToUpdate, conversationThreadRequested.get());
        }

        return createResponse(email, conversationId, postBox);
    }

    private Optional<PostBoxSingleConversationThreadResponse> createResponse(String email, String conversationId, PostBox postBox) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return Optional.empty();
        }

        Optional<PostBoxSingleConversationThreadResponse> created =
                PostBoxSingleConversationThreadResponse.create(postBox.getNewRepliesCounter().getValue(), email, conversation);
        if (created.isPresent()) {
            return created;
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return Optional.empty();
        }
    }

    private PostBox updatePostBox(String email, String conversationId, PostBox postBox) {
        int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
        postBox.decrementNewReplies(unreadMessages);
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        List<ConversationThread> cthread = postBox.getConversationThreads();
        for (ConversationThread item : cthread) {

            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {

                threadsToUpdate.add(new ConversationThread(
                        item.getAdId(),
                        item.getConversationId(),
                        item.getCreatedAt(),
                        now(),
                        item.getReceivedAt(),
                        false, // mark as read
                        item.getPreviewLastMessage(),
                        item.getBuyerName(),
                        item.getSellerName(),
                        item.getBuyerId(),
                        item.getMessageDirection(),
                        item.getUserIdBuyer(),
                        item.getUserIdSeller())
                );

            } else {
                threadsToUpdate.add(item);
            }
        }

        return new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
    }
}
