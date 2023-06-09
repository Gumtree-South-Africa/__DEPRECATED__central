package com.ecg.messagecenter.it.webapi;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
import com.ecg.messagecenter.core.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.it.persistence.ConversationThread;
import com.ecg.messagecenter.it.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Controller class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private final SimpleMessageCenterRepository postBoxRepository;
    private final ConversationRepository conversationRepository;

    @Autowired public ConversationThreadController(ConversationRepository conversationRepository,
                                                   SimpleMessageCenterRepository postBoxRepository) {
        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }

    @InitBinder public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
                    produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET,
                    RequestMethod.PUT}) @ResponseBody
    ResponseObject<?> getPostBoxConversationByEmailAndConversationId(
                    @PathVariable("email") String email,
                    @PathVariable("conversationId") String conversationId,
                    @RequestParam(value = "newCounterMode", defaultValue = "true")
                    boolean newCounterMode,
                    @RequestParam(value = "robotEnabled", defaultValue = "true", required = false)
                    boolean robotEnabled, HttpServletRequest request,
                    HttpServletResponse response) {
        PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested =
                        postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response);
        }

        boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();
        if (needToMarkAsRead) {
            int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
            postBox.decrementNewReplies(unreadMessages);
            postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
        }


        if (newCounterMode) {
            if (needToMarkAsRead) {
                markConversationAsRead(email, conversationId, postBox);
            }
            return lookupConversation(postBox.getNewRepliesCounter().getValue(), email,
                            conversationId, response, robotEnabled);
        } else {
            long numUnread;
            if (needToMarkAsRead) {
                numUnread = markConversationAsRead(email, conversationId, postBox);
            } else {
                numUnread = postBox.getUnreadConversationsCapped().size();
            }
            return lookupConversation(numUnread, email, conversationId, response, robotEnabled);
        }
    }

    private ResponseObject<?> lookupConversation(long numUnread, String email,
                    String conversationId, HttpServletResponse response, boolean robotEnabled) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket",
                            conversationId);
            return entityNotFound(response);
        }

        Optional<PostBoxSingleConversationThreadResponse> created =
                        PostBoxSingleConversationThreadResponse
                                        .create(numUnread, email, conversation, robotEnabled);
        if (created.isPresent()) {
            return ResponseObject.of(created.get());
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI",
                            conversationId);
            return entityNotFound(response);
        }

    }

    private long markConversationAsRead(String email, String conversationId, PostBox<ConversationThread> postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        boolean needsUpdate = false;
        ConversationThread updatedConversation = null;
        for (ConversationThread item : postBox.getConversationThreads()) {
            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                updatedConversation = new ConversationThread(item.getAdId(),
                        item.getConversationId(),
                        item.getCreatedAt(),
                        now(),
                        item.getReceivedAt(),
                        false, // mark as read
                        item.getPreviewLastMessage(),
                        item.getBuyerName(),
                        item.getSellerName(),
                        item.getBuyerId(),
                        item.getMessageDirection());

                threadsToUpdate.add(updatedConversation);
                needsUpdate = true;
            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox<ConversationThread> postBoxToUpdate = new PostBox<>(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
            postBoxRepository.markConversationAsRead(postBoxToUpdate, updatedConversation);
            numUnreadCounter = postBoxToUpdate.getUnreadConversationsCapped().size();
        } else {
            numUnreadCounter = postBox.getUnreadConversationsCapped().size();
        }

        return numUnreadCounter;
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}
