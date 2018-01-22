package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.requests.SystemMessagePayload;
import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.controllers.responses.converters.ConversationResponseConverter;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationController {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationController.class);

    private final Timer getConversationTimer = TimingReports.newTimer("webapi.get-conversation");
    private final Timer markConversationAsReadTimer = TimingReports.newTimer("webapi.mark-conversation-as-read");
    private final Timer postSystemMessage = TimingReports.newTimer("webapi.post-system-message");

    private final PostBoxService postBoxService;
    private final ConversationResponseConverter responseConverter;

    @Autowired
    public ConversationController(PostBoxService postBoxService, ConversationResponseConverter responseConverter) {
        this.postBoxService = postBoxService;
        this.responseConverter = responseConverter;
    }

    @GetMapping("/users/{userId}/conversations/{conversationId}")
    public ResponseEntity<ResponseObject<?>> getConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(name = "cursor", required = false) Optional<String> messageIdCursorOpt,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

        LOG.trace("Retrieving conversation with conversationID: {}, userId: {}", conversationId, userId);
        try (Timer.Context ignored = getConversationTimer.time()) {
            Optional<ConversationResponse> conversationResponse = postBoxService
                    .getConversation(userId, conversationId, messageIdCursorOpt, limit)
                    .map(responseConverter::toConversationResponseWithMessages);
            if (!conversationResponse.isPresent()) {
                LOG.trace("Conversation not found with conversationID: {}, userId: {}", conversationId, userId);
            }
            return wrapResponse(conversationResponse);
        }
    }

    @PostMapping("/users/{userId}/conversations/{conversationId}")
    public ResponseEntity<ResponseObject<?>> performActionOnConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam("action") String action,
            @RequestParam(name = "cursor", required = false) Optional<String> messageIdCursorOpt,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

        switch (action) {
            case "mark-as-read":
                try (Timer.Context ignored = markConversationAsReadTimer.time()) {
                    Optional<ConversationResponse> conversationResponse = postBoxService
                            .markConversationAsRead(userId, conversationId, messageIdCursorOpt, limit)
                            .map(responseConverter::toConversationResponseWithMessages);
                    return wrapResponse(conversationResponse);
                }
            default:
                return ResponseEntity.ok(ResponseObject.of(RequestState.INVALID_ARGUMENTS));
        }
    }

    @PostMapping("/users/{userId}/conversations/{conversationId}/system-messages")
    public ResponseObject<?> postSystemMessage(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @Valid @RequestBody SystemMessagePayload payload,
            BindingResult bindingResult) {

        try (Timer.Context ignored = postSystemMessage.time()) {
            if (bindingResult.hasErrors()) {
                LOG.info("Invalid arguments. {}", bindingResult.getAllErrors());
                return ResponseObject.of(RequestState.INVALID_ARGUMENTS);
            }
            postBoxService.createSystemMessage(userId, conversationId, payload.getAdId(), payload.getText(), payload.getCustomData(), payload.isSendPush());
            return ResponseObject.of(RequestState.OK);
        }
    }

    private ResponseEntity<ResponseObject<?>> wrapResponse(Optional<ConversationResponse> conversationResponseOpt) {
        return conversationResponseOpt.isPresent()
                ? ResponseEntity.ok(ResponseObject.of(conversationResponseOpt.get()))
                : entityNotFound();
    }

    private ResponseEntity<ResponseObject<?>> entityNotFound() {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.of(RequestState.ENTITY_NOT_FOUND));
    }
}