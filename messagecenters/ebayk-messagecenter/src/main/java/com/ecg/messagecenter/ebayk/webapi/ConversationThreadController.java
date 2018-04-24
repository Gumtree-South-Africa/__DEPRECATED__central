package com.ecg.messagecenter.ebayk.webapi;

import com.ecg.messagecenter.ebayk.diff.ConversationService;
import com.ecg.messagecenter.ebayk.diff.WebApiSyncService;
import com.ecg.messagecenter.ebayk.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationThreadController {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationThreadController.class);

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

    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseEntity<ResponseObject<PostBoxSingleConversationThreadResponse>> readConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

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

    private ResponseEntity entityNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.of(RequestState.ENTITY_NOT_FOUND));
    }
}
