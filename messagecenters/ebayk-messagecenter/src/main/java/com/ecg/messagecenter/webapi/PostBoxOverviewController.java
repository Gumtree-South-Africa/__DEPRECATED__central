package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.diff.WebApiSyncService;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostBoxOverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer API_POSTBOX_CONVERSATION_DELETE_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    private final boolean syncEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository,
            @Value("${webapi.sync.ek.enabled:false}") boolean syncEnabled) {

        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " in SyncMode");
        }
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/postboxes/{email:.+}")
    public ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50") Integer size,
            @RequestParam(value = "page", defaultValue = "0") Integer page) {

        try (Timer.Context ignore = API_POSTBOX_BY_EMAIL.time()) {
            PostBoxResponse postBoxResponse;
            if (syncEnabled) {
                // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
                postBoxResponse = webapiSyncService.getPostBox(email, page, size);
            } else {
                PostBox postBox;
                if (size != 0) {
                    postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                } else {
                    postBox = postBoxRepository.byIdWithoutConversationThreads(PostBoxId.fromEmail(email));
                }
                API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());
                postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox);
            }

            return ResponseObject.of(postBoxResponse);
        }
    }

    @PutMapping("/postboxes/{email:.+}")
    public ResponseObject<PostBoxResponse> readPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50") Integer size,
            @RequestParam(value = "page", defaultValue = "0") Integer page) {

        try (Timer.Context ignored = API_POSTBOX_BY_EMAIL.time()) {
            PostBoxResponse postBoxResponse;
            if (syncEnabled) {
                // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
                postBoxResponse = webapiSyncService.readPostBox(email, page, size);
            } else {
                PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

                List conversations = postBox.getConversationThreadsCapTo(page, size);
                int totalUnreads = postBoxRepository.unreadCountInConversations(postBox.getId(), conversations);
                postBox.decrementNewReplies(totalUnreads);

                postBoxRepository.markConversationsAsRead(postBox, conversations);
                postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox);
            }

            return ResponseObject.of(postBoxResponse);
        }
    }

    @DeleteMapping("/postboxes/{email}/conversations")
    public ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
            @PathVariable("email") String email,
            @RequestParam(value = "ids", defaultValue = "") String[] ids,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {

        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_DELETE_BY_ID.time()) {
            PostBoxResponse postBoxResponse;
            if (syncEnabled) {
                // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
                postBoxResponse = webapiSyncService.deleteConversations(email, Arrays.asList(ids), page, size);
            } else {
                PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

                for (String id : ids) {
                    postBox.removeConversation(id);
                }

                postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
                postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox);
            }

            return ResponseObject.of(postBoxResponse);
        }
    }
}
