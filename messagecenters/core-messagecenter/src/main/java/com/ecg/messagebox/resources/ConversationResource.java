package com.ecg.messagebox.resources;

import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.requests.SystemMessagePayload;
import com.ecg.messagebox.resources.responses.ConversationResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.base.Preconditions;
import io.swagger.annotations.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

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

        return postBoxService
                .getConversation(userId, conversationId, messageIdCursor, limit)
                .map(ConversationResponseConverter::toConversationResponseWithMessages)
                .orElseThrow(() -> new ClientException(HttpStatus.NOT_FOUND, String.format("Conversation not found for ID: %s", conversationId)));
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

    @ApiOperation(value = "Delete a single conversation", notes = "Remove a single conversation for a user", nickname = "deleteConversationForUser", tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @DeleteMapping("/users/{userId}/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversationForUser(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId) {

        try {

            MutableConversation conversation = conversationRepository.getById(conversationId);
            if (conversation == null) {
                return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
            }

            PreconditionIssuerEmailIsBuyerOrSeller(changeConversationStatePayload, conversation);

            conversation.applyCommand(
                    new ConversationClosedCommand(
                            conversationId,
                            ConversationRole.getRole(changeConversationStatePayload.getIssuerEmail(), conversation),
                            DateTime.now()
                    )
            );

            ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

            return ResponseObject.of(RequestState.OK);



            LOG.trace("Deleting conversation with conversationID: {} for user with userId: {}", conversationId, userId);
            postBoxService.deleteConversation(userId, conversationId, "-1");
        } catch (Exception e) {
            LOG.error("Error encountered: " + e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
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

        postBoxService.createSystemMessage(userId, conversationId, payload.getAdId(), payload.getText(), payload.getCustomData(), payload.isSendPush());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}