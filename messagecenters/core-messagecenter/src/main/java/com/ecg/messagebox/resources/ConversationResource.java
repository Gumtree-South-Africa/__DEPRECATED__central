package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.ConversationResponseConverter;
import com.ecg.messagebox.controllers.requests.SystemMessagePayload;
import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
public class ConversationResource {

    private final Timer getConversationTimer = TimingReports.newTimer("webapi.get-conversation");
    private final Timer markConversationAsReadTimer = TimingReports.newTimer("webapi.mark-conversation-as-read");
    private final Timer postSystemMessage = TimingReports.newTimer("webapi.post-system-message");

    private final PostBoxService postBoxService;

    @Autowired
    public ConversationResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    /*
     * After Spring-Boot upgrade to a higher version of spring-web
     * - make it using Optional flow API
     * - the higher version contains better support of generics in ResponseEntity
     * - ResponseEntity<ConversationResponse>
     */
    @ApiOperation(value = "Get a single conversation", notes = "Retrieve a single conversation along with messages")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResponse.class),
            @ApiResponse(code = 404, message = "Conversation Not Found")
    })
    @GetMapping("/users/{userId}/conversations/{conversationId}")
    public ResponseEntity getConversation(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "ID of the first message returned in a response") @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @ApiParam(value = "Number of messages returned in a response") @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = getConversationTimer.time()) {
            Optional<ConversationResponse> conversationResponse = postBoxService
                    .getConversation(userId, conversationId, messageIdCursor, limit)
                    .map(ConversationResponseConverter::toConversationResponseWithMessages);

            if (conversationResponse.isPresent()) {
                return ResponseEntity.ok(conversationResponse.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }

    /*
     * After Spring-Boot upgrade to a higher version of spring-web
     * - make it using Optional flow API
     * - the higher version contains better support of generics in ResponseEntity
     * - ResponseEntity<ConversationResponse>
     */
    @ApiOperation(value = "Read a single conversation", notes = "Mark a single conversation as read")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success", response = ConversationResponse.class),
            @ApiResponse(code = 404, message = "Conversation Not Found")
    })
    @PutMapping("/users/{userId}/conversations/{conversationId}/read")
    public ResponseEntity readConversation(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "ID of the first message returned in a response") @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @ApiParam(value = "Number of messages returned in a response") @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = markConversationAsReadTimer.time()) {
            Optional<ConversationResponse> conversationResponse = postBoxService
                    .markConversationAsRead(userId, conversationId, messageIdCursor, limit)
                    .map(ConversationResponseConverter::toConversationResponseWithMessages);

            if (conversationResponse.isPresent()) {
                return ResponseEntity.ok(conversationResponse.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        }
    }

    @ApiOperation(value = "Post a system message", notes = "System message is posted to User ID and Conversation ID specified in the path")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Created")
    })
    @PostMapping("/users/{userId}/conversations/{conversationId}/system-messages")
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