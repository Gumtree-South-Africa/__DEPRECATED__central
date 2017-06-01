package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Controller
class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        new TopLevelExceptionHandler(ex, response).handle();
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<?> getPostBoxConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletRequest request,
            HttpServletResponse response) {

        Timer.Context timerContext = API_POSTBOX_CONVERSATION_BY_ID.time();

        try {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound(response);
            }

            if (shouldMarkAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages()) {

                PostBox postBoxToUpdate = updatePostBox(email, conversationId, postBox);
                postBoxRepository.write(postBoxToUpdate);
            }

            Conversation conversation = conversationRepository.getById(conversationId);
            // can only happen if both buckets diverge
            if (conversation == null) {
                LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
                return entityNotFound(response);
            }

            Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(postBox.getNewRepliesCounter().getValue(),
                    email, conversation);
            if (created.isPresent()) {
                API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
                return ResponseObject.of(created.get());
            } else {
                LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
                return entityNotFound(response);
            }


        } finally {
            timerContext.stop();
        }
    }

    private PostBox updatePostBox(String email, String conversationId, PostBox postBox) {
        postBox.decrementNewReplies();
        List<ConversationThread> threadsToUpdate = new ArrayList<ConversationThread>();

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

        return new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate, maxAgeDays);
    }


    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }

    private boolean shouldMarkAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}
