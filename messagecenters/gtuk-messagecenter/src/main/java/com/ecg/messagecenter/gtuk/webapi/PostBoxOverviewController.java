package com.ecg.messagecenter.gtuk.webapi;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
import com.ecg.messagecenter.core.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.gtuk.diff.WebApiSyncService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostBoxOverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private final SimpleMessageCenterRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
    private final boolean syncEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            SimpleMessageCenterRepository postBoxRepository,
            @Value("${webapi.sync.uk.enabled:false}") boolean syncEnabled) {

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

    @GetMapping(value = MessageCenterGetPostBoxCommand.MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "newCounterMode", defaultValue = "false") boolean newCounterMode,
            @RequestParam(value = "size", defaultValue = "50") Integer size,
            @RequestParam(value = "page", defaultValue = "0") Integer page) {

        PostBoxResponse postBoxResponse;
        if (syncEnabled) {
            // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
            postBoxResponse = webapiSyncService.getPostBox(email, page, size, newCounterMode);
        } else {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
            postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
        }

        return ResponseObject.of(postBoxResponse);
    }
}