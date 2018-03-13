package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.diff.WebApiSyncService;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationThreadController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadController.class);
    private static final Timer POSTBOX_CONVERSATION_GET = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Timer POSTBOX_CONVERSATION_MARK_READ = TimingReports.newTimer("webapi-postbox-conversation-mark-read");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    private final SimplePostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final boolean syncEnabled;
    private final boolean diffEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public ConversationThreadController(
            SimplePostBoxRepository postBoxRepository,
            ConversationRepository conversationRepository,
            @Value("${webapi.sync.au.enabled:false}") boolean syncEnabled,
            @Value("${webapi.diff.au.enabled:false}") boolean diffEnabled
    ) {
        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.syncEnabled = syncEnabled;
        this.diffEnabled = diffEnabled;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseObject getPostBoxConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletResponse response
    ) {

        try (Timer.Context ignored = POSTBOX_CONVERSATION_GET.time()) {
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

            if (syncEnabled && diffEnabled && responseObject.getBody() instanceof PostBoxSingleConversationThreadResponse) {
                webapiSyncService.logDiffOnConversationGet(email, conversationId, (PostBoxSingleConversationThreadResponse) responseObject.getBody());
            }

            return responseObject;
        }
    }

    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseObject markReadPostBoxConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletResponse response
    ) {
        try (Timer.Context ignored = POSTBOX_CONVERSATION_MARK_READ.time()) {
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

            if (syncEnabled && responseObject.getBody() instanceof PostBoxSingleConversationThreadResponse) {
                webapiSyncService.readConversation(email, conversationId, (PostBoxSingleConversationThreadResponse) responseObject.getBody());
            }

            return responseObject;
        }
    }

    private ResponseObject lookupConversation(long numUnread, String email, String conversationId, HttpServletResponse response) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            return entityNotFound(response, conversationId);
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation);
        if (created.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
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
