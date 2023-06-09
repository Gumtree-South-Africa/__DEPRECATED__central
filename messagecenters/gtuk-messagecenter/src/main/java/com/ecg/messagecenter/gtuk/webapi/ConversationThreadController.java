package com.ecg.messagecenter.gtuk.webapi;

import com.ecg.messagecenter.core.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.gtuk.diff.ConversationService;
import com.ecg.messagecenter.gtuk.diff.WebApiSyncService;
import com.ecg.messagecenter.gtuk.webapi.requests.MessageCenterDeleteConversationCommand;
import com.ecg.messagecenter.gtuk.webapi.requests.MessageCenterReportConversationCommand;
import com.ecg.messagecenter.gtuk.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
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

    private final ConversationService conversationService;
    private final boolean syncEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public ConversationThreadController(ConversationService conversationService,
                                        @Value("${webapi.sync.uk.enabled:false}") boolean syncEnabled) {
        this.conversationService = conversationService;
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode");
        }
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    /*
     * newCounterMode is never used, default is always used = true
     */
    @GetMapping(MessageCenterGetPostBoxConversationCommand.MAPPING)
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

    /*
     * newCounterMode is never used, default is always used = true
     */
    @PutMapping(MessageCenterGetPostBoxConversationCommand.MAPPING)
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

    @DeleteMapping(MessageCenterDeleteConversationCommand.MAPPING)
    public ResponseEntity<ResponseObject<ConversationRts>> deleteConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        Optional<ConversationRts> response;
        if (syncEnabled) {
            response = webapiSyncService.deleteConversation(email, conversationId);
        } else {
            response = conversationService.deleteConversation(email, conversationId);
        }

        return response.map(ResponseObject::of)
                .map(ResponseEntity::ok)
                .orElseGet(this::entityNotFound);
    }

    @PostMapping(MessageCenterReportConversationCommand.MAPPING)
    public ResponseEntity<ResponseObject> reportConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        return conversationService.reportConversation(email, conversationId)
                .map(obj -> new ResponseObject())
                .map(ResponseEntity::ok)
                .orElseGet(this::entityNotFound);
    }

    private ResponseEntity entityNotFound() {
        return new ResponseEntity<>(ResponseObject.of(RequestState.ENTITY_NOT_FOUND), HttpStatus.NOT_FOUND);
    }
}