package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.kjca.sync.WebApiSyncService;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponseBuilder;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class PostBoxOverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    private final boolean syncEnabled;

    @Autowired
    public PostBoxOverviewController(
            SimplePostBoxRepository postBoxRepository,
            ConversationBlockRepository conversationBlockRepository,
            @Value("${replyts.maxConversationAgeDays:180}") int maxAgeDays,
            @Value("${webapi.sync.ca.enabled:false}") boolean syncEnabled) {

        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationBlockRepository, maxAgeDays);
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode");
        }
    }

    @GetMapping("/postboxes/{email:.+}")
    ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "role", required = false) ConversationRole role) {

        PostBox postBox;
        if (syncEnabled) {
            postBox = webapiSyncService.getPostBox(email, size, page);
        } else {
            postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        }

        return responseBuilder.buildPostBoxResponse(email, size, page, role, postBox);
    }
}
