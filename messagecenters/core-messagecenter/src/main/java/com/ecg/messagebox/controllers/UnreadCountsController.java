package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.converters.UnreadCountsResponseConverter;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.ecg.replyts.core.runtime.TimingReports.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
public class UnreadCountsController {

    private static final Timer GET_UNREAD_COUNTS_TIMER = newTimer("webapi.get-unread-counts");

    private static final String UNREAD_COUNTS_RESOURCE = "/users/{userId}/unread-counts";

    private final PostBoxService postBoxService;
    private final UnreadCountsResponseConverter responseConverter;

    @Autowired
    public UnreadCountsController(PostBoxService postBoxService,
                                  UnreadCountsResponseConverter responseConverter) {
        this.postBoxService = postBoxService;
        this.responseConverter = responseConverter;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        TopLevelExceptionHandler.handle(ex, response);
    }

    @RequestMapping(value = UNREAD_COUNTS_RESOURCE, method = GET)
    @ResponseBody
    public ResponseObject<?> getUnreadCounts(@PathVariable("userId") String userId) {
        try (Timer.Context ignored = GET_UNREAD_COUNTS_TIMER.time()) {
            UserUnreadCounts unreadCounts = postBoxService.getUnreadCounts(userId);
            return ResponseObject.of(responseConverter.toUnreadCountsResponse(unreadCounts));
        }
    }
}