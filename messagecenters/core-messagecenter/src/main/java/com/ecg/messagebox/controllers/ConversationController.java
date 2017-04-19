package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.converters.ConversationResponseConverter;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class ConversationController {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationController.class);

    private static final String CONVERSATION_RESOURCE = "/users/{userId}/conversations/{conversationId}";

    private final Timer getConversationTimer = TimingReports.newTimer("webapi.get-conversation");
    private final Timer markConversationAsReadTimer = TimingReports.newTimer("webapi.mark-conversation-as-read");

    private final PostBoxService postBoxService;
    private final ConversationResponseConverter responseConverter;

    @Autowired
    public ConversationController(PostBoxService postBoxService,
                                  ConversationResponseConverter responseConverter) {
        this.postBoxService = postBoxService;
        this.responseConverter = responseConverter;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = CONVERSATION_RESOURCE, produces = APPLICATION_JSON_UTF8_VALUE, method = GET)
    @ResponseBody
    ResponseObject<?> getConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "cursor", required = false) Optional<String> messageIdCursorOpt,
            @RequestParam(value = "limit", defaultValue = "500", required = false) int limit,
            HttpServletResponse response
    ) {
        LOG.trace("Retrieving conversation with conversationID: {}, userId: {}", conversationId, userId);
        try (Timer.Context ignored = getConversationTimer.time()) {
            Optional<ConversationResponse> conversationResponse = postBoxService
                    .getConversation(userId, conversationId, messageIdCursorOpt, limit)
                    .map(responseConverter::toConversationResponseWithMessages);
            LOG.trace("Conversation not found with conversationID: {}, userId: {}", conversationId, userId);
            return wrapResponse(conversationResponse, response);
        }
    }

    @RequestMapping(value = CONVERSATION_RESOURCE, produces = APPLICATION_JSON_UTF8_VALUE, method = POST)
    @ResponseBody
    ResponseObject<?> markConversationAsRead(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "action") String action,
            @RequestParam(value = "cursor", required = false) Optional<String> messageIdCursorOpt,
            @RequestParam(value = "limit", defaultValue = "500", required = false) int limit,
            HttpServletResponse response
    ) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {
            if (action.equals("mark-as-read")) {
                Optional<ConversationResponse> conversationResponse = postBoxService
                        .markConversationAsRead(userId, conversationId, messageIdCursorOpt, limit)
                        .map(responseConverter::toConversationResponseWithMessages);
                return wrapResponse(conversationResponse, response);
            } else {
                return ResponseObject.of(RequestState.INVALID_ARGUMENTS);
            }
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