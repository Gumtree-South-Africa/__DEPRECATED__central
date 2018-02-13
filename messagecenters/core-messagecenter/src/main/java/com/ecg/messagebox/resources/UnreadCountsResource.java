package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.UnreadCountsResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@RestController
public class UnreadCountsResource {

    private static final Timer GET_UNREAD_COUNTS_TIMER = newTimer("webapi.get-unread-counts");

    private final PostBoxService postBoxService;

    @Autowired
    public UnreadCountsResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @GetMapping("/users/{userId}/unread-counts")
    public UnreadCountsResponse getUnreadCounts(@PathVariable("userId") String userId) {
        try (Timer.Context ignored = GET_UNREAD_COUNTS_TIMER.time()) {
            UserUnreadCounts unreadCounts = postBoxService.getUnreadCounts(userId);
            return toUnreadCountsResponse(unreadCounts);
        }
    }

    private static UnreadCountsResponse toUnreadCountsResponse(UserUnreadCounts unreadCounts) {
        return new UnreadCountsResponse(
                unreadCounts.getUserId(),
                unreadCounts.getNumUnreadConversations(),
                unreadCounts.getNumUnreadMessages());
    }
}
