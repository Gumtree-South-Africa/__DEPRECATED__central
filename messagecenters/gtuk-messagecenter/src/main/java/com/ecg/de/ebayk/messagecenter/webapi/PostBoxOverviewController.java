package com.ecg.de.ebayk.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.de.ebayk.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
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

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:06
 *
 * @author maldana@ebay.de
 */
@Controller
class PostBoxOverviewController {

    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer API_POSTBOX_CONVERSATION_DELETE_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX =
            TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final PostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired
    public PostBoxOverviewController(
            ConversationRepository conversationRepository,
            PostBoxRepository postBoxRepository) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
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
            produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "newCounterMode", defaultValue = "false") boolean newCounterMode,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            HttpServletRequest request) {

        Timer.Context timerContext = API_POSTBOX_BY_EMAIL.time();

        try {
            PostBox postBox = postBoxRepository.byId(email);

            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX.update(postBox.getConversationThreads().size());

            if (markAsRead(request)) {
                postBox.resetReplies();
                postBoxRepository.write(postBox);
            }

            return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);

        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = MessageCenterDeletePostBoxConversationCommandNew.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
            @PathVariable("email") String email,
            @RequestParam(value = "ids", defaultValue = "") String[] ids,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {

        Timer.Context timerContext = API_POSTBOX_CONVERSATION_DELETE_BY_ID.time();

        try {
            PostBox postBox = postBoxRepository.byId(email);

            for (String id : ids) {
                postBox.removeConversation(id);
            }

            postBoxRepository.write(postBox);

            return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);

        } finally {
            timerContext.stop();
        }
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }

}