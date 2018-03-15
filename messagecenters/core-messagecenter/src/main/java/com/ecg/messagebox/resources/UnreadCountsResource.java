package com.ecg.messagebox.resources;

import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.UnreadCountsResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UnreadCountsResource {

    private final PostBoxService postBoxService;

    @Autowired
    public UnreadCountsResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @ApiOperation(
            value = "Get unread counts",
            notes = "Get a number of unread messages and conversations belonging to a specified user",
            nickname = "getUnreadCounts",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = UnreadCountsResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @GetMapping("/users/{userId}/unread-counts")
    public UnreadCountsResponse getUnreadCounts(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId) {
        UserUnreadCounts unreadCounts = postBoxService.getUnreadCounts(userId);
        return toUnreadCountsResponse(unreadCounts);
    }

    private static UnreadCountsResponse toUnreadCountsResponse(UserUnreadCounts unreadCounts) {
        return new UnreadCountsResponse(
                unreadCounts.getUserId(),
                unreadCounts.getNumUnreadConversations(),
                unreadCounts.getNumUnreadMessages());
    }
}
