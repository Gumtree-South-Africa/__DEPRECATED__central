package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.controllers.responses.converters.ConversationsResponseConverter;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class ConversationsController {

    private static final String CONVERSATIONS_RESOURCE = "/users/{userId}/conversations";
    private static final String CONVERSATION_IDS_BY_ADID_RESOURCE = "/users/{userId}/ads/{adId}/conversations/ids";

    private final Timer getConversationsTimer = TimingReports.newTimer("webapi.get-conversations");
    private final Timer executeActionsTimer = TimingReports.newTimer("webapi.execute-actions");
    private final Timer getConversationIdsByAdId = TimingReports.newTimer("webapi.get-conversation-ids-by-adid");

    private final PostBoxService postBoxService;
    private final ConversationsResponseConverter responseConverter;

    @Autowired
    public ConversationsController(PostBoxService postBoxService,
                                   ConversationsResponseConverter responseConverter) {
        this.postBoxService = postBoxService;
        this.responseConverter = responseConverter;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = CONVERSATIONS_RESOURCE, produces = APPLICATION_JSON_VALUE, method = GET)
    @ResponseBody
    ResponseObject<?> getConversations(
            @PathVariable("userId") String userId,
            @RequestParam(value = "offset", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "50", required = false) int limit,
            @RequestParam(value = "visibility", defaultValue = "active", required = false) String visibility
    ) {
        try (Timer.Context ignored = getConversationsTimer.time()) {
            PostBox conversations = postBoxService.getConversations(userId, Visibility.valueOf(visibility.toUpperCase()), offset, limit);
            ConversationsResponse conversationsResponse = responseConverter.toConversationsResponse(conversations, offset, limit);
            return ResponseObject.of(conversationsResponse);
        }
    }

    @RequestMapping(value = CONVERSATIONS_RESOURCE, produces = APPLICATION_JSON_VALUE, method = POST)
    @ResponseBody
    ResponseObject<?> executeActions(
            @PathVariable("userId") String userId,
            @RequestParam("action") String action,
            @RequestParam(value = "visibility", defaultValue = "ACTIVE", required = false) String visibility,
            @RequestParam(value = "ids", required = false) String[] conversationIds,
            @RequestParam(value = "offset", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "50", required = false) int limit
    ) {
        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox;
            Visibility newVisibility = Visibility.valueOf(visibility.toUpperCase());
            switch (action) {
                case "change-visibility":
                    Visibility toggledVisibility = Visibility.ARCHIVED.equals(newVisibility) ? Visibility.ACTIVE : Visibility.ARCHIVED;
                    postBox = postBoxService.changeConversationVisibilities(userId, Arrays.asList(conversationIds), newVisibility, toggledVisibility, offset, limit);
                    break;
                default:
                    postBox = null;
                    break;
            }
            Object response = postBox == null ? RequestState.INVALID_ARGUMENTS : responseConverter.toConversationsResponse(postBox, offset, limit);
            return ResponseObject.of(response);
        }
    }

    @RequestMapping(value = CONVERSATION_IDS_BY_ADID_RESOURCE, produces = APPLICATION_JSON_VALUE, method = GET)
    @ResponseBody
    ResponseObject<?> getConversationIds(
            @PathVariable("userId") String userId,
            @PathVariable("adId") String adId,
            @RequestParam(value = "limit", defaultValue = "500", required = false) int limit
    ) {
        try (Timer.Context ignored = getConversationIdsByAdId.time()) {
            List<String> resolvedConversationIds = postBoxService
                    .resolveConversationIdByUserIdAndAdId(userId, adId, limit);
            return ResponseObject.of(resolvedConversationIds);
        }
    }


}