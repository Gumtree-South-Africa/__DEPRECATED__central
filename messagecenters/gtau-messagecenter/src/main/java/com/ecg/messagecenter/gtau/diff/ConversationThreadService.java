package com.ecg.messagecenter.gtau.diff;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.gtau.persistence.ConversationThread;
import com.ecg.messagecenter.gtau.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Component
public class ConversationThreadService {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadService.class);

    private final ConversationRepository conversationRepository;
    private final SimplePostBoxRepository postBoxRepository;

    @Autowired
    public ConversationThreadService(ConversationRepository conversationRepository, SimplePostBoxRepository postBoxRepository) {
        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }

    public ResponseObject getPostBoxConversation(String email, String conversationId, boolean newCounterMode, HttpServletResponse response) {

        ResponseObject responseObject;
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response, conversationId);
        }

        if (newCounterMode) {
            responseObject = lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, response);
        } else {
            long numUnread = postBox.getUnreadConversationsCapped().size();
            responseObject = lookupConversation(numUnread, email, conversationId, response);
        }

        return responseObject;
    }

    public ResponseObject markReadPostBoxConversation(String email, String conversationId, boolean newCounterMode, HttpServletResponse response) {
        ResponseObject responseObject;
        PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response, conversationId);
        }

        ConversationThread conversationThread = (ConversationThread) conversationThreadRequested.get();
        boolean needToMarkAsRead = conversationThread.isContainsUnreadMessages();
        if (needToMarkAsRead) {
            LOG.debug("Marking conversation with ID {} as read", conversationId);
            int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
            postBox.decrementNewReplies(unreadMessages);
            postBoxRepository.markConversationAsRead(postBox, conversationThread);
        }

        if (newCounterMode) {
            if (needToMarkAsRead) {
                markConversationAsRead(email, conversationId, postBox);
            }
            responseObject = lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, response);
        } else {
            long numUnread = needToMarkAsRead ? markConversationAsRead(email, conversationId, postBox) : postBox.getUnreadConversationsCapped().size();
            responseObject = lookupConversation(numUnread, email, conversationId, response);
        }

        return responseObject;
    }

    private ResponseObject lookupConversation(long numUnread, String email, String conversationId, HttpServletResponse response) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            return entityNotFound(response, conversationId);
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation);
        if (created.isPresent()) {
            return ResponseObject.of(created.get());
        } else {
            return entityNotFound(response, conversationId);
        }
    }

    private long markConversationAsRead(String email, String conversationId, PostBox<ConversationThread> postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        boolean needsUpdate = false;
        ConversationThread updatedConversation = null;
        for (ConversationThread item : postBox.getConversationThreads()) {

            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                updatedConversation = new ConversationThread(
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
                        item.getSellerId(),
                        item.getMessageDirection(),
                        item.getRobot(),
                        item.getOfferId(),
                        item.getLastMessageAttachments(),
                        item.getLastMessageId(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty());

                threadsToUpdate.add(updatedConversation);
                needsUpdate = true;
            } else {
                threadsToUpdate.add(item);
            }
        }

        //optimization to not cause too many write actions (potential for conflicts)
        long numUnreadCounter;
        if (needsUpdate) {
            PostBox<AbstractConversationThread> postBoxToUpdate = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
            postBoxRepository.markConversationAsRead(postBoxToUpdate, updatedConversation);
            numUnreadCounter = postBoxToUpdate.getUnreadConversationsCapped().size();
        } else {
            numUnreadCounter = postBox.getUnreadConversationsCapped().size();
        }

        return numUnreadCounter;
    }

    private static ResponseObject<?> entityNotFound(HttpServletResponse response, String conversationId) {
        LOG.debug("Conversation with ID {} was not found", conversationId);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }
}
