package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.webapi.requests.PostBoxConversationCommand;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

@Controller
class ConversationController {

    private static final Timer API_POSTBOX_CONVERSATION_TIMER = TimingReports.newTimer("webapi-postbox-conversation-by-id");

    private final PostBoxService postBoxServiceDelegator;

    @Autowired
    public ConversationController(@Qualifier("postBoxServiceDelegator") PostBoxService postBoxServiceDelegator) {
        this.postBoxServiceDelegator = postBoxServiceDelegator;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = PostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    ResponseObject<?> getConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_TIMER.time()) {
            return wrapResponse(postBoxServiceDelegator.getConversation(userId, conversationId), response);
        }
    }

    @RequestMapping(value = PostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.PUT)
    @ResponseBody
    ResponseObject<?> markConversationAsRead(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_TIMER.time()) {
            return wrapResponse(postBoxServiceDelegator.markConversationAsRead(userId, conversationId), response);
        }
    }

    private ResponseObject<?> wrapResponse(Optional<ConversationResponse> conversationResponseOpt, HttpServletResponse response) {
        return conversationResponseOpt.isPresent() ? ResponseObject.of(conversationResponseOpt.get()) : entityNotFound(response);
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }
}