package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.diff.ConversationThreadService;
import com.ecg.messagecenter.diff.WebApiSyncService;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationThreadController {

    private final ConversationThreadService conversationThreadService;
    private final boolean syncEnabled;
    private final boolean diffEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public ConversationThreadController(
            ConversationThreadService conversationThreadService,
            @Value("${webapi.sync.au.enabled:false}") boolean syncEnabled,
            @Value("${webapi.diff.au.enabled:false}") boolean diffEnabled) {
        this.conversationThreadService = conversationThreadService;
        this.syncEnabled = syncEnabled;
        this.diffEnabled = diffEnabled;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseObject getPostBoxConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletResponse response) {
        ResponseObject responseObject = conversationThreadService.getPostBoxConversation(email, conversationId, newCounterMode, response);
        if (syncEnabled && diffEnabled && responseObject.getBody() instanceof PostBoxSingleConversationThreadResponse) {
            webapiSyncService.logDiffOnConversationGet(email, conversationId, (PostBoxSingleConversationThreadResponse) responseObject.getBody());
        }

        return responseObject;
    }

    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseObject markReadPostBoxConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletResponse response) {
        ResponseObject responseObject = conversationThreadService.markReadPostBoxConversation(email, conversationId, newCounterMode, response);
        if (syncEnabled && responseObject.getBody() instanceof PostBoxSingleConversationThreadResponse) {
            webapiSyncService.readConversation(email, conversationId, (PostBoxSingleConversationThreadResponse) responseObject.getBody());
        }

        return responseObject;
    }
}
