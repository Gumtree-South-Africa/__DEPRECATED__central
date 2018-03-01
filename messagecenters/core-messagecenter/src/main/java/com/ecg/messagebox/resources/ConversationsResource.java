package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.resources.responses.ConversationsResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationsResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationsResource.class);

    private final Timer getConversationsTimer = TimingReports.newTimer("webapi.get-conversations");
    private final Timer executeActionsTimer = TimingReports.newTimer("webapi.execute-actions");
    private final Timer getConversationIdsByAdId = TimingReports.newTimer("webapi.get-conversation-ids-by-adid");

    private final PostBoxService postBoxService;

    @Autowired(required = false)
    private WebApiSyncV2Service webApiSyncV2Service;

    @Autowired
    public ConversationsResource(PostBoxService postBoxService, @Value("${webapi.sync.v2.enabled:false}") boolean syncEnabled) {
        this.postBoxService = postBoxService;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode");
        }
    }

    @ApiOperation(
            value = "Get conversations",
            notes = "Retrieve a collection of conversations beloging to a specified User ID",
            nickname = "getConversations",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
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

    @ApiOperation(
            value = "Archive conversations",
            notes = "Mark conversations as archived",
            nickname = "archiveConversations",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PutMapping(path = "/users/{userId}/conversations/archive", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConversationsResponse archiveConversations(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam("Index of the first conversation in user's postbox") @RequestParam(name = "offset", defaultValue = "0") int offset,
            @ApiParam("Type of the conversations returned in the response") @RequestParam(name = "limit", defaultValue = "50") int limit,
            @ApiParam(value = "List of configuration ids", required = true) @RequestBody List<String> conversationIds) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            if (webApiSyncV2Service == null) {
                PostBox postBox = postBoxService.archiveConversations(userId, conversationIds, offset, limit);
                return toConversationsResponse(postBox, offset, limit);
            } else {
                /*
                 * PB: Only for migration to V2 then delete this code.
                 * - This method is mutator and we have to sync calls from to v2 -> v1
                 * to provide tenants some way how to revert migration and go back to v1
                 * otherwise V1 won't contain changes made in V2.
                 */
                PostBox postBox = webApiSyncV2Service.archiveConversations(userId, conversationIds, offset, limit);
                return toConversationsResponse(postBox, offset, limit);
            }
        }
    }

    @ApiOperation(
            value = "Activate conversations",
            notes = "Mark conversations as active",
            nickname = "activateConversations",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationsResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PutMapping(path = "/users/{userId}/conversations/activate", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ConversationsResponse activateConversations(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam("Index of the first conversation in user's postbox") @RequestParam(name = "offset", defaultValue = "0") int offset,
            @ApiParam("Type of the conversations returned in the response") @RequestParam(name = "limit", defaultValue = "50") int limit,
            @ApiParam(value = "List of configuration ids", required = true) @RequestBody List<String> conversationIds) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox = postBoxService.activateConversations(userId, conversationIds, offset, limit);
            return toConversationsResponse(postBox, offset, limit);
        }
    }

    @ApiOperation(
            value = "Get conversations' IDs belonging to AD ID",
            notes = "Retrieve a list of conversations' IDs belonging to a specified AD ID",
            nickname = "getConversationIds",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
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