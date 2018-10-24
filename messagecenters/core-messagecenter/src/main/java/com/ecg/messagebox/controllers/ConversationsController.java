package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationsController {

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

        PostBox conversations = postBoxService.getConversations(userId, Visibility.valueOf(visibility.toUpperCase()), offset, limit);
        ConversationsResponse conversationsResponse = toConversationsResponse(conversations, offset, limit);
        return ResponseObject.of(conversationsResponse);
    }

    @PostMapping("/users/{userId}/conversations")
    public ResponseObject<?> executeActions(
            @PathVariable("userId") String userId,
            @RequestParam("action") String action,
            @RequestParam(name = "visibility", defaultValue = "ACTIVE") String visibility,
            @RequestParam(name = "ids") String[] conversationIds,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) throws InterruptedException {

        PostBox postBox;
        Visibility newVisibility = Visibility.valueOf(visibility.toUpperCase());
        switch (action) {
            case "change-visibility":
                if (newVisibility == Visibility.ACTIVE) {
                    postBox = postBoxService.activateConversations(userId, Arrays.asList(conversationIds), offset, limit);
                } else {
                    postBox = postBoxService.archiveConversations(userId, Arrays.asList(conversationIds), offset, limit);
                }
                break;
            default:
                postBox = null;
                break;
        }
        Object response = postBox == null ? RequestState.INVALID_ARGUMENTS : toConversationsResponse(postBox, offset, limit);
        return ResponseObject.of(response);
    }

    @GetMapping("/users/{userId}/ads/{adId}/conversations/ids")
    public ResponseObject<?> getConversationIds(
            @PathVariable("userId") String userId,
            @PathVariable("adId") String adId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

        List<String> resolvedConversationIds = postBoxService
                .getConversationsById(userId, adId, limit);
        return ResponseObject.of(resolvedConversationIds);
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