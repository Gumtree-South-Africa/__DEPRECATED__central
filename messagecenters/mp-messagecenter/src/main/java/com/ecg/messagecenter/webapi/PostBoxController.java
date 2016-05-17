package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.webapi.requests.DeleteConversationsCommand;
import com.ecg.messagecenter.webapi.requests.GetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

import static java.util.Arrays.asList;

@Controller
class PostBoxController {

    private static final Timer GET_POSTBOX_TIMER = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer DELETE_CONVERSATIONS_TIMER = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private final PostBoxService postBoxService;

    @Autowired
    public PostBoxController(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = GetPostBoxCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET})
    @ResponseBody
    ResponseObject<PostBoxResponse> getPostBox(
            @PathVariable String postBoxId,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page) {

        Timer.Context timerContext = GET_POSTBOX_TIMER.time();
        try {
            return ResponseObject.of(postBoxService.getConversations(postBoxId, size, page));
        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = GetPostBoxCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.PUT})
    @ResponseBody
    ResponseObject<?> markConversationsAsRead(
            @PathVariable String postBoxId,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page) {

        Timer.Context timerContext = GET_POSTBOX_TIMER.time();
        try {
            PostBoxResponse postBoxResponse = postBoxService.markConversationsAsRead(postBoxId, size, page);
            return postBoxResponse == null ? ResponseObject.of(RequestState.OK) : ResponseObject.of(postBoxResponse);
        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = DeleteConversationsCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<?> deleteConversations(
            @PathVariable("postBoxId") String postBoxId,
            @RequestParam(value = "ids", defaultValue = "") String[] conversationIds,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {

        Timer.Context timerContext = DELETE_CONVERSATIONS_TIMER.time();
        try {
            PostBoxResponse postBoxResponse = postBoxService.deleteConversations(postBoxId, asList(conversationIds), page, size);
            return postBoxResponse == null ? ResponseObject.of(RequestState.OK) : ResponseObject.of(postBoxResponse);
        } finally {
            timerContext.stop();
        }
    }
}