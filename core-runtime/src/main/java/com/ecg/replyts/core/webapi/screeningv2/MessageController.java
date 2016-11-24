package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.ModerateMessageCommand;
import com.ecg.replyts.core.api.webapi.commands.SearchMessageCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.ModerateMessagePayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.google.common.base.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * controller handling all message related tasks.
 */
@Controller
public class MessageController {

    private final ModerationService moderationService;
    private final DomainObjectConverter converter;
    private final SearchService searchService;
    private final ConversationRepository conversationRepository;
    private final OutdatedEntityMonitor entityMonitor;


    @Autowired
    MessageController(ModerationService moderationService, DomainObjectConverter converter, SearchService searchService, ConversationRepository conversationRepository, OutdatedEntityMonitor entityMonitor) {
        this.moderationService = moderationService;
        this.converter = converter;
        this.searchService = searchService;
        this.conversationRepository = conversationRepository;
        this.entityMonitor = entityMonitor;
    }

    /**
     * Sets a message to a new state. Messages are identified by their id and their conversation's id.
     */
    @RequestMapping(value = ModerateMessageCommand.MAPPING, method = POST, consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject<?> changeMessageState(@PathVariable String conversationId,
                                                @PathVariable String messageId,
                                                @RequestBody ModerateMessagePayload mmc) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null)
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND, String.format("conversation with id %s not found", conversationId));

        Message message = conversation.getMessageById(messageId);
        if (message == null)
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND, String.format("message with id %s in conversation %s not found", messageId, conversationId));

        ModerationResultState newState = mmc.getNewMessageState();
        if (!newState.isAcceptableUserDecision()) {
            throw new IllegalStateException("Moderation to " + newState + " is not allowed. Try either GOOD or BAD");
        }

        MessageState currentMessageState = mmc.getCurrentMessageState();
        if (currentMessageState != null && !currentMessageState.equals(message.getState())) {
            return ResponseObject.of(RequestState.ENTITY_OUTDATED, String.format("The state of the message with id %s has already changed", messageId));
        }

        Optional<String> editor = Optional.fromNullable(mmc.getEditor());

        moderationService.changeMessageState(conversation, messageId, new ModerationAction(newState, editor));

        return ResponseObject.success("Set message state to " + newState);
    }


    /**
     * Performs a message search. search command must be described in the post payload.
     */
    @RequestMapping(value = SearchMessageCommand.MAPPING, consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject<?> searchMessages(@RequestBody SearchMessagePayload command) {

        RtsSearchResponse response = searchService.search(command);
        List<RtsSearchResponse.IDHolder> idHolderList = response.getResult();
        List<MessageRts> messageResults = converter.convertFromSearchResults(idHolderList);
        ResponseObject<List<MessageRts>> responseObject = ResponseObject.of(messageResults);

        // check if the current search result contains "outdated" entities (that should have been reindexed already)
        // this is the case after crashes or split brains. if this is the case, "repair" them in the index by reindexing.
        if (command.getMessageStates() != null) {
            if (entityMonitor.canOutdate(command.getMessageStates())) {
                entityMonitor.scan(responseObject.getBody(), command.getMessageStates());
            }
        }

        if (response.isPartialResult()) {
            responseObject.setPagination(response.getOffset(), response.getCount(), response.getTotal());
        }
        return responseObject;
    }
}
