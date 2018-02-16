package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.ConversationResponseConverter;
import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ConversationsResource {

    private final Timer getConversationsTimer = TimingReports.newTimer("webapi.get-conversations");
    private final Timer executeActionsTimer = TimingReports.newTimer("webapi.execute-actions");
    private final Timer getConversationIdsByAdId = TimingReports.newTimer("webapi.get-conversation-ids-by-adid");

    private final PostBoxService postBoxService;

    @Autowired
    public ConversationsResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @ApiOperation(value = "Get conversations", notes = "Retrieve a collection of conversations beloging to a specified User ID")
    @ApiResponses(@ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class))
    @GetMapping("/users/{userId}/conversations")
    public ConversationsResponse getConversations(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam("Index of the first conversation in user's postbox") @RequestParam(name = "offset", defaultValue = "0") int offset,
            @ApiParam("Number of conversations returned in the response") @RequestParam(name = "limit", defaultValue = "50") int limit,
            @ApiParam("Type of the conversations returned in the response") @RequestParam(name = "visibility", defaultValue = "active") Visibility visibility) {

        try (Timer.Context ignored = getConversationsTimer.time()) {
            PostBox conversations = postBoxService.getConversations(userId, visibility, offset, limit);
            return toConversationsResponse(conversations, offset, limit);
        }
    }

    @ApiOperation(value = "Archive conversations", notes = "Mark conversations as archived")
    @ApiResponses(@ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class))
    @PutMapping("/users/{userId}/conversations/archive")
    public ConversationsResponse archiveConversations(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "List of configuration divided by commas", required = true) @RequestParam(name = "ids") String[] conversationIds,
            @ApiParam("Index of the first conversation in user's postbox") @RequestParam(name = "offset", defaultValue = "0") int offset,
            @ApiParam("Type of the conversations returned in the response") @RequestParam(name = "limit", defaultValue = "50") int limit) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox = postBoxService.archiveConversations(userId, Arrays.asList(conversationIds), offset, limit);
            return toConversationsResponse(postBox, offset, limit);
        }
    }

    @ApiOperation(value = "Activate conversations", notes = "Mark conversations as active")
    @ApiResponses(@ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class))
    @PutMapping("/users/{userId}/conversations/activate")
    public ConversationsResponse activateConversations(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "List of configuration divided by commas", required = true) @RequestParam(name = "ids") String[] conversationIds,
            @ApiParam("Index of the first conversation in user's postbox") @RequestParam(name = "offset", defaultValue = "0") int offset,
            @ApiParam("Type of the conversations returned in the response") @RequestParam(name = "limit", defaultValue = "50") int limit) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox = postBoxService.activateConversations(userId, Arrays.asList(conversationIds), offset, limit);
            return toConversationsResponse(postBox, offset, limit);
        }
    }

    @ApiOperation(value = "Get conversations' IDs belonging to AD ID", notes = "Retrieve a list of conversations' IDs belonging to a specified AD ID")
    @ApiResponses(@ApiResponse(code = 200, message = "Success"))
    @GetMapping("/users/{userId}/ads/{adId}/conversations/ids")
    public List<String> getConversationIds(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "AD ID", required = true) @PathVariable("adId") String adId,
            @ApiParam("Maximum number of IDs returned in a response") @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = getConversationIdsByAdId.time()) {
            return postBoxService.getConversationsById(userId, adId, limit);
        }
    }

    private static ConversationsResponse toConversationsResponse(PostBox postBox, int offset, int limit) {
        return new ConversationsResponse(
                postBox.getUserId(),
                postBox.getUnreadCounts().getNumUnreadMessages(),
                postBox.getUnreadCounts().getNumUnreadConversations(),
                postBox.getConversations().stream().map(ConversationResponseConverter::toConversationResponse).collect(Collectors.toList()),
                offset,
                limit,
                postBox.getConversationsTotalCount());
    }
}