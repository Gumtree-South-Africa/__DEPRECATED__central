package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.kjca.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.messagecenter.kjca.sync.ConversationService;
import com.ecg.messagecenter.kjca.sync.WebApiSyncService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class ConversationThreadController {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadController.class);

    private final ConversationService conversationService;
    private final boolean syncEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public ConversationThreadController(ConversationService conversationService,
                                        @Value("${webapi.sync.ca.enabled:false}") boolean syncEnabled) {
        this.conversationService = conversationService;
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode");
        }
    }

    @GetMapping(value = "/postboxes/{email}/conversations/{conversationId}")
    ResponseObject<?> getPostBox(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse httpResponse) {

        Optional<PostBoxSingleConversationThreadResponse> response = conversationService.getConversation(email, conversationId);

        if (response.isPresent()) {
            return ResponseObject.of(response.get());
        } else {
            LOG.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound(httpResponse);
        }
    }

    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    ResponseObject<?> readPostBox(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse httpResponse) {

        Optional<PostBoxSingleConversationThreadResponse> response = syncEnabled
                ? webapiSyncService.readConversation(email, conversationId)
                : conversationService.readConversation(email, conversationId);

        if (response.isPresent()) {
            return ResponseObject.of(response.get());
        } else {
            LOG.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound(httpResponse);
        }
    }

    @DeleteMapping("/postboxes/{email}/conversations/{conversationId}")
    ResponseEntity<Void> deleteSingleConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") String conversationId) {

        if (syncEnabled) {
            webapiSyncService.deleteConversation(email, conversationId);
        } else {
            conversationService.deleteConversation(email, conversationId);
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }
}
