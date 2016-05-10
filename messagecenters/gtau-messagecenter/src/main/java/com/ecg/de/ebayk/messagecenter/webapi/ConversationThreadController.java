package com.ecg.de.ebayk.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.de.ebayk.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.joda.time.DateTime.now;

@Controller
class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");


    private final PostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public ConversationThreadController(
            ConversationRepository conversationRepository,
            PostBoxRepository postBoxRepository) {

        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }


    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
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
            PostBox postBox = postBoxRepository.byId(email);

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound(response);
            }

            boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();
            if(needToMarkAsRead) {
                postBox.decrementNewReplies();
                postBoxRepository.write(postBox);
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
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return entityNotFound(response);
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation, robotEnabled);
        if (created.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
            return ResponseObject.of(created.get());
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound(response);
        }

    }

    private long markConversationAsRead(String email, String conversationId, PostBox postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<ConversationThread>();

        boolean needsUpdate = false;
        for (ConversationThread item : postBox.getConversationThreads()) {

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
                        item.getRobot(),
                        item.getOfferId()));

                needsUpdate = true;

            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox postBoxToUpdate = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
            postBoxRepository.write(postBoxToUpdate);
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
