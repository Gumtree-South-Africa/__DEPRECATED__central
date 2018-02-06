package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.diff.WebApiSyncService;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationThreadController {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");

    private final boolean syncEnabled;
    private final ConversationService conversationService;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public ConversationThreadController(ConversationService conversationService,
                                        @Value("${webapi.sync.ek.enabled:false}") boolean syncEnabled) {
        this.conversationService = conversationService;
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " in SyncMode");
        }
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseEntity<ResponseObject<PostBoxSingleConversationThreadResponse>> getConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        try (Timer.Context ignore = API_POSTBOX_CONVERSATION_BY_ID.time()) {
            Optional<PostBoxSingleConversationThreadResponse> response;
            if (syncEnabled) {
                response = webapiSyncService.getConversation(email, conversationId);
            } else {
                response = conversationService.getConversation(email, conversationId);
            }

            return response.map(ResponseObject::of)
                    .map(ResponseEntity::ok)
                    .orElseGet(this::entityNotFound);
        }
    }

    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseEntity<ResponseObject<PostBoxSingleConversationThreadResponse>> readConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        try (Timer.Context ignore = API_POSTBOX_CONVERSATION_BY_ID.time()) {
            Optional<PostBoxSingleConversationThreadResponse> response;
            if (syncEnabled) {
                response = webapiSyncService.readConversation(email, conversationId);
            } else {
                response = conversationService.readConversation(email, conversationId);
            }

            return response.map(ResponseObject::of)
                    .map(ResponseEntity::ok)
                    .orElseGet(this::entityNotFound);
        }
    }

    private ResponseEntity entityNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.of(RequestState.ENTITY_NOT_FOUND));
    }
}
