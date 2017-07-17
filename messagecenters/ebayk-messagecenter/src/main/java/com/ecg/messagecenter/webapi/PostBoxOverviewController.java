package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@RestController
public class PostBoxOverviewController {
    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer API_POSTBOX_CONVERSATION_DELETE_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired
    public PostBoxOverviewController(ConversationRepository conversationRepository, SimplePostBoxRepository postBoxRepository) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        TopLevelExceptionHandler.handle(ex, response);
    }

    @RequestMapping(
            value = MessageCenterGetPostBoxCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            method = RequestMethod.GET)
    public ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page) {

        try (Timer.Context ignored = API_POSTBOX_BY_EMAIL.time()) {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());
            return responseBuilder.buildPostBoxResponse(email, size, page, postBox);
        }
    }

    @RequestMapping(
            value = MessageCenterGetPostBoxCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            method = RequestMethod.PUT)
    public ResponseObject<PostBoxResponse> readPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page) {

        try (Timer.Context ignored = API_POSTBOX_BY_EMAIL.time()) {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());
            postBox.resetReplies();
            postBoxRepository.markConversationsAsRead(postBox);
            return responseBuilder.buildPostBoxResponse(email, size, page, postBox);
        }
    }

    @RequestMapping(
            value = MessageCenterDeletePostBoxConversationCommandNew.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            method = RequestMethod.DELETE)
    public ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
            @PathVariable("email") String email,
            @RequestParam(value = "ids", defaultValue = "") String[] ids,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {

        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_DELETE_BY_ID.time()) {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            for (String id : ids) {
                postBox.removeConversation(id);
            }

            postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
            return responseBuilder.buildPostBoxResponse(email, size, page, postBox);
        }
    }
}
