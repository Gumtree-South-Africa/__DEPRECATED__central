package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.requests.EmptyConversationRequest;
import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationsController {

    private final Timer getConversationsTimer = TimingReports.newTimer("webapi.get-conversations");
    private final Timer executeActionsTimer = TimingReports.newTimer("webapi.execute-actions");
    private final Timer getConversationIdsByAdId = TimingReports.newTimer("webapi.get-conversation-ids-by-adid");
    private final Timer postEmptyConversation = TimingReports.newTimer("webapi.post-empty-conversation");

    private final PostBoxService postBoxService;

    @Autowired
    public ConversationsController(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/users/{userId}/conversations")
    public ResponseObject<?> getConversations(
            @PathVariable("userId") String userId,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "visibility", defaultValue = "active") String visibility) {

        try (Timer.Context ignored = getConversationsTimer.time()) {
            PostBox conversations = postBoxService.getConversations(userId, Visibility.valueOf(visibility.toUpperCase()), offset, limit);
            ConversationsResponse conversationsResponse = toConversationsResponse(conversations, offset, limit);
            return ResponseObject.of(conversationsResponse);
        }
    }

    @PostMapping("/users/{userId}/conversations")
    public ResponseObject<?> executeActions(
            @PathVariable("userId") String userId,
            @RequestParam("action") String action,
            @RequestParam(name = "visibility", defaultValue = "ACTIVE") String visibility,
            @RequestParam(name = "ids") String[] conversationIds,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox;
            Visibility newVisibility = Visibility.valueOf(visibility.toUpperCase());
            switch (action) {
                case "change-visibility":
                    Visibility toggledVisibility = Visibility.ARCHIVED.equals(newVisibility) ? Visibility.ACTIVE : Visibility.ARCHIVED;
                    postBox = postBoxService.changeConversationVisibilities(userId, Arrays.asList(conversationIds), newVisibility, toggledVisibility, offset, limit);
                    break;
                default:
                    postBox = null;
                    break;
            }
            Object response = postBox == null ? RequestState.INVALID_ARGUMENTS : toConversationsResponse(postBox, offset, limit);
            return ResponseObject.of(response);
        }
    }

    @GetMapping("/users/{userId}/ads/{adId}/conversations/ids")
    public ResponseObject<?> getConversationIds(
            @PathVariable("userId") String userId,
            @PathVariable("adId") String adId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = getConversationIdsByAdId.time()) {
            List<String> resolvedConversationIds = postBoxService
                    .resolveConversationIdByUserIdAndAdId(userId, adId, limit);
            return ResponseObject.of(resolvedConversationIds);
        }
    }

    @PostMapping("/users/{userId}/ads/{adId}")
    public ResponseEntity createEmptyConversation(@Valid @RequestBody EmptyConversationRequest emptyConversation, BindingResult bindingResult) {
        try (Timer.Context ignored = postEmptyConversation.time()) {
            if (bindingResult.hasErrors()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            Optional<String> conversationId = postBoxService.createEmptyConversation(emptyConversation);
            return conversationId.isPresent()
                    ? new ResponseEntity<>(ResponseObject.of(conversationId.get()), HttpStatus.OK)
                    : new ResponseEntity<>("Missing participant for buyer or seller", HttpStatus.BAD_REQUEST);
        }
    }


    private static ConversationsResponse toConversationsResponse(PostBox postBox, int offset, int limit) {
        return new ConversationsResponse(
                postBox.getUserId(),
                postBox.getUnreadCounts().getNumUnreadMessages(),
                postBox.getUnreadCounts().getNumUnreadConversations(),
                postBox.getConversations().stream().map(ConversationResponseConverter::toConversationResponse).collect(Collectors.toList()),
                offset,
                limit,
                postBox.getConversationsTotalCount()
        );
    }
}