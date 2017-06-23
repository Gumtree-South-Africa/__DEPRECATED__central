package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.util.ConversationThreadEnricher;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
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
import java.util.Arrays;

@Controller
public class PostBoxOverviewController {
    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer API_POSTBOX_CONVERSATION_DELETE_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository,
            ConversationThreadEnricher conversationThreadEnricher) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository, conversationThreadEnricher);
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterGetPostBoxCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "newCounterMode", defaultValue = "false") boolean newCounterMode,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "robotEnabled", defaultValue = "true", required = false) boolean robotEnabled,
            HttpServletRequest request) {

        Timer.Context timerContext = API_POSTBOX_BY_EMAIL.time();

        try {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

            if (markAsRead(request)) {
                postBox.resetReplies();
                postBoxRepository.write(postBox);
            }

            if (robotEnabled) {
                return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
            } else {
                return responseBuilder.buildPostBoxResponseRobotExcluded(email, size, page, postBox, newCounterMode);
            }

        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = MessageCenterDeletePostBoxConversationCommandNew.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
            @PathVariable("email") String email,
            @RequestParam(value = "ids", defaultValue = "") String[] ids,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {
        Timer.Context timerContext = API_POSTBOX_CONVERSATION_DELETE_BY_ID.time();

        try {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            for (String id : ids) {
                postBox.removeConversation(id);
            }

            postBoxRepository.write(postBox, Arrays.asList(ids));

            return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);

        } finally {
            timerContext.stop();
        }
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}