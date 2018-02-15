package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.ConversationResponseConverter;
import com.ecg.messagebox.controllers.requests.SystemMessagePayload;
import com.ecg.messagebox.controllers.responses.ConversationResponse;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

@RestController
public class ConversationResource {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationResource.class);

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
    @GetMapping("/users/{userId}/conversations/{conversationId}")
    public ResponseEntity getConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

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
    @PutMapping("/users/{userId}/conversations/{conversationId}/read")
    public ResponseEntity readConversation(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(name = "cursor", required = false) String messageIdCursor,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

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

    @PostMapping("/users/{userId}/conversations/{conversationId}/system-messages")
    public ResponseEntity<Void> postSystemMessage(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @Valid @RequestBody SystemMessagePayload payload) {

        try (Timer.Context ignored = postSystemMessage.time()) {
            postBoxService.createSystemMessage(userId, conversationId, payload.getAdId(), payload.getText(), payload.getCustomData(), payload.isSendPush());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
    }
}