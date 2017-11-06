package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.diff.DiffReporter;
import com.ecg.messagecenter.diff.WebApiDiffService;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@RestController
public class PostBoxOverviewController {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX =
            TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;
    private final boolean diffEnabled;

    @Autowired(required = false)
    @Qualifier("webApiDiffService")
    private WebApiDiffService webapiDiffService;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository,
            @Value("${webapi.diff.uk.enabled:false}") boolean diffEnabled) {

        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
        this.diffEnabled = diffEnabled;

        if (diffEnabled) {
            LOG.info(DiffReporter.DIFF_MARKER, this.getClass().getSimpleName() + " in Diffing mode");
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

        try (Timer.Context ignore = API_POSTBOX_BY_EMAIL.time()) {
            PostBoxResponse postBoxResponse;
            if (diffEnabled) {
                // Use retrieved PostBox Response from diffing to avoid the second call to cassandra
                postBoxResponse = webapiDiffService.getPostBox(PostBoxId.fromEmail(email), page, size, newCounterMode);
            } else {
                PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());
                postBoxResponse = responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
            }

            return ResponseObject.of(postBoxResponse);
        }
    }
}