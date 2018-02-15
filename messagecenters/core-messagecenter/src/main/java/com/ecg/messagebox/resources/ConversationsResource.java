package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.ConversationResponseConverter;
import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ConversationsResource {

    private final Timer getConversationsTimer = TimingReports.newTimer("webapi.get-conversations");
    private final Timer executeActionsTimer = TimingReports.newTimer("webapi.execute-actions");
    private final Timer getConversationIdsByAdId = TimingReports.newTimer("webapi.get-conversation-ids-by-adid");

    private final PostBoxService postBoxService;

    @Autowired
    public ConversationsResource(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @GetMapping("/users/{userId}/conversations")
    public ConversationsResponse getConversations(
            @PathVariable("userId") String userId,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "visibility", defaultValue = "active") String visibility) {

        try (Timer.Context ignored = getConversationsTimer.time()) {
            PostBox conversations = postBoxService.getConversations(userId, Visibility.valueOf(visibility.toUpperCase()), offset, limit);
            return toConversationsResponse(conversations, offset, limit);
        }
    }

    @PutMapping("/users/{userId}/conversations/archive")
    public ConversationsResponse archiveConversations(
            @PathVariable("userId") String userId,
            @RequestParam(name = "ids") String[] conversationIds,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox = postBoxService.archiveConversations(userId, Arrays.asList(conversationIds), offset, limit);
            return toConversationsResponse(postBox, offset, limit);
        }
    }

    @PutMapping("/users/{userId}/conversations/activate")
    public ConversationsResponse activateConversations(
            @PathVariable("userId") String userId,
            @RequestParam(name = "ids") String[] conversationIds,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {

        try (Timer.Context ignored = executeActionsTimer.time()) {
            PostBox postBox = postBoxService.activateConversations(userId, Arrays.asList(conversationIds), offset, limit);
            return toConversationsResponse(postBox, offset, limit);
        }
    }

    @GetMapping("/users/{userId}/ads/{adId}/conversations/ids")
    public List<String> getConversationIds(
            @PathVariable("userId") String userId,
            @PathVariable("adId") String adId,
            @RequestParam(name = "limit", defaultValue = "500") int limit) {

        try (Timer.Context ignored = getConversationIdsByAdId.time()) {
            return postBoxService.getConversationsById(userId, adId, limit);
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
                postBox.getConversationsTotalCount());
    }
}