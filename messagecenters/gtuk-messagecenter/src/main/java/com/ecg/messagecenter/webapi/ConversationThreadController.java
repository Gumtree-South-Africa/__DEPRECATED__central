package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.diff.DiffReporter;
import com.ecg.messagecenter.diff.WebApiDiffService;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeleteConversationCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterReportConversationCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final boolean diffEnabled;

    @Autowired(required = false)
    @Qualifier("webApiDiffService")
    private WebApiDiffService webapiDiffService;

    @Autowired
    public ConversationThreadController(ConversationService conversationService,
                                        @Value("${webapi.diff.uk.enabled:false}") boolean diffEnabled) {
        this.conversationService = conversationService;
        this.diffEnabled = diffEnabled;

        if (diffEnabled) {
            LOG.info(DiffReporter.DIFF_MARKER, "PostBoxOverviewController in Diffing mode");
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
        if (diffEnabled) {
            response = webapiDiffService.getConversation(email, conversationId);
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
        if (diffEnabled) {
            response = webapiDiffService.readConversation(email, conversationId);
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
        if (diffEnabled) {
            response = webapiDiffService.deleteConversation(email, conversationId);
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