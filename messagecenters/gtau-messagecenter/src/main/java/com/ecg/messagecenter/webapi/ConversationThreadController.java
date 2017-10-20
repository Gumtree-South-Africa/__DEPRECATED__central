package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Controller
public class ConversationThreadController {
    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<?> getPostBoxConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            @RequestParam(value ="robotEnabled", defaultValue = "true", required = false) boolean robotEnabled,
            HttpServletRequest request,
            HttpServletResponse response) {

        Timer.Context timerContext = API_POSTBOX_CONVERSATION_BY_ID.time();

        try {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound(response);
            }

            boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();
            if(needToMarkAsRead) {
                int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
                postBox.decrementNewReplies(unreadMessages);
                postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
            }

            if(newCounterMode) {
                if(needToMarkAsRead) {
                    markConversationAsRead(email, conversationId, postBox);
                }
                return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, response, robotEnabled);
            } else {
                long numUnread;
                if (needToMarkAsRead) {
                    numUnread = markConversationAsRead(email, conversationId, postBox);
                } else {
                    numUnread = postBox.getUnreadConversationsCapped().size();
                }
                return lookupConversation(numUnread, email, conversationId, response, robotEnabled);
            }

        } finally {
            timerContext.stop();
        }
    }

    private ResponseObject<?> lookupConversation(long numUnread, String email, String conversationId, HttpServletResponse response, boolean robotEnabled) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            return entityNotFound(response);
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation, robotEnabled);
        if (created.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
            return ResponseObject.of(created.get());
        } else {
            return entityNotFound(response);
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

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox<AbstractConversationThread> postBoxToUpdate = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
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
