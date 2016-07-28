package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.PostBoxUnreadCountsResponse;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

@Controller
public class UnreadCountsController {

    private static final Timer API_POSTBOX_GET_UNREAD_COUNTS = TimingReports.newTimer("webapi-postbox-get-unread-counts");

    private static final String MAPPING = "/postboxes/{postBoxId}/unread-counters";

    private final PostBoxService postBoxServiceDelegator;

    @Autowired
    public UnreadCountsController(@Qualifier("postBoxServiceDelegator") PostBoxService postBoxServiceDelegator) {
        this.postBoxServiceDelegator = postBoxServiceDelegator;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MAPPING, method = RequestMethod.GET)
    @ResponseBody
    public ResponseObject<PostBoxUnreadCountsResponse> getUnreadCounts(@PathVariable String postBoxId) {
        Timer.Context timerContext = API_POSTBOX_GET_UNREAD_COUNTS.time();
        try {
            PostBoxUnreadCounts unreadCounts = postBoxServiceDelegator.getUnreadCounts(postBoxId);
            return ResponseObject.of(new PostBoxUnreadCountsResponse(unreadCounts));
        } finally {
            timerContext.stop();
        }
    }
}