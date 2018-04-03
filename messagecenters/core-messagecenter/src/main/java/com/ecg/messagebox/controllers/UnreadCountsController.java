package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.UnreadCountsResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UnreadCountsController {

    private final PostBoxService postBoxService;

    @Autowired
    public UnreadCountsController(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @GetMapping("/users/{userId}/unread-counts")
    public ResponseObject<?> getUnreadCounts(@PathVariable("userId") String userId) {
        UserUnreadCounts unreadCounts = postBoxService.getUnreadCounts(userId);
        return ResponseObject.of(toUnreadCountsResponse(unreadCounts));
    }

    private static UnreadCountsResponse toUnreadCountsResponse(UserUnreadCounts unreadCounts) {
        return new UnreadCountsResponse(
                unreadCounts.getUserId(),
                unreadCounts.getNumUnreadConversations(),
                unreadCounts.getNumUnreadMessages());
    }
}