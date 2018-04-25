package com.ecg.messagecenter.bt.webapi;

import com.ecg.messagecenter.bt.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.bt.webapi.requests.MessageCenterClosePostBoxConversationCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.bt.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@Controller
public class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxOverviewController.class);

    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired
    private MailCloakingService mailCloakingService;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterClosePostBoxConversationCommand.MAPPING, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    @ResponseBody
    public ResponseObject<?> closePostBoxConversationByEmailAndConversationId(@PathVariable("email") String email, @PathVariable("conversationId") String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);

        if (conversation == null) {
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
        }

        if (conversation.getState() == ConversationState.CLOSED) {
            return ResponseObject.of(RequestState.OK);
        }

        conversation.applyCommand(new ConversationClosedCommand(conversationId, ConversationRole.getRole(email, conversation), DateTime.now()));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        String buyerEmail = conversation.getSellerId();
        String sellerEmail = conversation.getBuyerId();
        ConversationRole role = conversation.isClosedBy(ConversationRole.Buyer)?ConversationRole.Buyer:ConversationRole.Seller;

        updatePostBox(buyerEmail,conversationId,role);
        updatePostBox(sellerEmail,conversationId,role);

        return ResponseObject.of(RequestState.OK);
    }

    private void updatePostBox(String email, String conversationId, ConversationRole role){
        PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);

        if (conversationThreadRequested.isPresent()) {
            ConversationThread conversationThread = conversationThreadRequested.get();

            conversationThread.setConversationState(Optional.of(ConversationState.CLOSED));

            conversationThread.setCloseBy(Optional.of(role));

            postBoxRepository.write(postBox);
        }
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING, produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    public ResponseObject<?> getPostBoxConversationByEmailAndConversationId(@PathVariable("email") String email, @PathVariable("conversationId") String conversationId,
      @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
      HttpServletRequest request, HttpServletResponse response) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);

        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response);
        }

        boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();

        if (needToMarkAsRead) {
            postBox.decrementNewReplies(1);

            postBoxRepository.write(postBox);
            postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
        }

        Map<String, String> customValues = conversationThreadRequested.get().getCustomValues().get();

        if (newCounterMode) {
            if (needToMarkAsRead) {
                markConversationAsRead(email, conversationId, postBox);
            }

            return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, response, customValues);
        } else {
            long numUnread;

            if (needToMarkAsRead) {
                numUnread = markConversationAsRead(email, conversationId, postBox);
            } else {
                numUnread = postBox.getUnreadConversations().size();
            }

            return lookupConversation(numUnread, email, conversationId, response, customValues);
        }
    }

    private ResponseObject<?> lookupConversation(long numUnread, String email, String conversationId,
                                                 HttpServletResponse response, Map<String, String> customValues) {
        Conversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);

            return entityNotFound(response);
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation, mailCloakingService, customValues);

        if (created.isPresent()) {
            return ResponseObject.of(created.get());
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);

            return entityNotFound(response);
        }

    }

    private long markConversationAsRead(String email, String conversationId, PostBox<ConversationThread> postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

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
                  item.getConversationState(),
                  item.getCloseBy(),
                  item.getCustomValues()));

                needsUpdate = true;
            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        // Optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox postBoxToUpdate = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);

            postBoxRepository.write(postBoxToUpdate);

            numUnreadCounter = postBoxToUpdate.getUnreadConversations().size();
        } else {
            numUnreadCounter = postBox.getUnreadConversations().size();
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
