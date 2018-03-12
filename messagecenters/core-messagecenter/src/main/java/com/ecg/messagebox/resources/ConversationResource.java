package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.requests.SystemMessagePayload;
import com.ecg.messagebox.resources.responses.ConversationResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

    private final Timer getConversationTimer = TimingReports.newTimer("webapi.get-conversation");
    private final Timer markConversationAsReadTimer = TimingReports.newTimer("webapi.mark-conversation-as-read");
    private final Timer postSystemMessage = TimingReports.newTimer("webapi.post-system-message");

    private final PostBoxService postBoxService;

    @Autowired(required = false)
    private WebApiSyncV2Service webApiSyncV2Service;

    @Autowired
    public ConversationResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @PostConstruct
    public void postConstruct() {
        if (webApiSyncV2Service != null) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode and Synchronization method is properly wired");
        }
    }

    @ApiOperation(value = "Get a single conversation", notes = "Retrieve a single conversation along with messages", nickname = "getConversation", tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResponse.class),
            @ApiResponse(code = 404, message = "Conversation Not Found", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @GetMapping("/users/{userId}/conversations/{conversationId}")
    public ConversationResponse getConversation(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "ID of the first message returned in a response") @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @ApiParam(value = "Number of messages returned in a response") @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = getConversationTimer.time()) {
            return postBoxService
                    .getConversation(userId, conversationId, messageIdCursor, limit)
                    .map(ConversationResponseConverter::toConversationResponseWithMessages)
                    .orElseThrow(() -> new ClientException(HttpStatus.NOT_FOUND, String.format("Conversation not found for ID: %s", conversationId)));
        }
    }

    @ApiOperation(
            value = "Read a single conversation",
            notes = "Mark a single conversation as read",
            nickname = "readConversation",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResponse.class),
            @ApiResponse(code = 404, message = "Conversation Not Found", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PutMapping("/users/{userId}/conversations/{conversationId}/read")
    public ConversationResponse readConversation(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "ID of the first message returned in a response") @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @ApiParam(value = "Number of messages returned in a response") @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = markConversationAsReadTimer.time()) {
            if (webApiSyncV2Service == null) {
                return postBoxService
                        .markConversationAsRead(userId, conversationId, messageIdCursor, limit)
                        .map(ConversationResponseConverter::toConversationResponseWithMessages)
                        .orElseThrow(() -> new ClientException(HttpStatus.NOT_FOUND, String.format("Conversation not found for ID: %s", conversationId)));
            } else {
                /*
                 * TODO PB: Only for migration to V2 then delete this code.
                 * - This method is mutator and we have to sync calls from to v2 -> v1
                 * to provide tenants some way how to revert migration and go back to v1
                 * otherwise V1 won't contain changes made in V2.
                 */
                return webApiSyncV2Service.markConversationAsRead(userId, conversationId, messageIdCursor, limit)
                        .map(ConversationResponseConverter::toConversationResponseWithMessages)
                        .orElseThrow(() -> new ClientException(HttpStatus.NOT_FOUND, String.format("Conversation not found for ID: %s", conversationId)));
            }
        }
    }

    @ApiOperation(
            value = "Post a system message",
            notes = "System message is posted to User ID and Conversation ID specified in the path",
            nickname = "postSystemMessage",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PostMapping(path = "/users/{userId}/conversations/{conversationId}/system-messages", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Void> postSystemMessage(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "System message payload", required = true) @Valid @RequestBody SystemMessagePayload payload) {

        try (Timer.Context ignored = postSystemMessage.time()) {
            postBoxService.createSystemMessage(userId, conversationId, payload.getAdId(), payload.getText(), payload.getCustomData(), payload.isSendPush());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }
}