package com.ecg.messagecenter.gtau.webapi;

import com.ecg.messagecenter.gtau.diff.WebApiSyncService;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.gtau.util.ConversationThreadEnricher;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.sync.PostBoxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostBoxOverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
    private final boolean syncEnabled;
    private final boolean diffEnabled;

    @Autowired(required = false)
    private WebApiSyncService webApiSyncService;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository,
            ConversationThreadEnricher conversationThreadEnricher,
            @Value("${webapi.sync.au.enabled:false}") boolean syncEnabled,
            @Value("${webapi.diff.au.enabled:false}") boolean diffEnabled) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository, conversationThreadEnricher);
        this.syncEnabled = syncEnabled;
        this.diffEnabled = diffEnabled;

        if (syncEnabled) {
            LOG.info("MessageCenter to MessageBox sync enabled");
        }

        if (diffEnabled) {
            LOG.info("Diff between MessageCenter and MessageBox enabled");
        }
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/postboxes/{email:.+}")
    public ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "newCounterMode", defaultValue = "false") boolean newCounterMode,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        ResponseObject<PostBoxResponse> postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);

        if (syncEnabled && diffEnabled) {
            webApiSyncService.logDiffOnPostBoxGet(email, page, size, postBoxResponse.getBody());
        }

        return postBoxResponse;
    }

    @DeleteMapping("/postboxes/{email}/conversations")
    public ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
            @PathVariable("email") String email,
            @RequestParam(value = "ids", defaultValue = "") String[] ids,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {
        ResponseObject<PostBoxResponse> postBoxResponse;
        if (syncEnabled) {
            // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
            postBoxResponse = webApiSyncService.deleteConversations(email, Arrays.asList(ids), page, size, newCounterMode);
        } else {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
            for (String id : ids) {
                postBox.removeConversation(id);
            }

            postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
            postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
        }

        return postBoxResponse;
    }
}
