package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.requests.CreateConversationRequest;
import com.ecg.messagebox.controllers.requests.PostMessageRequest;
import com.ecg.messagebox.controllers.responses.CreateConversationResponse;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.PostMessageResponse;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostMessageResource {

    private final Timer createConversationTimer = TimingReports.newTimer("webapi.create-conversation");
    private final Timer postMessageTimer = TimingReports.newTimer("webapi.post-message");

    private final ProcessingContextFactory processingContextFactory;
    private final MessageProcessingCoordinator messageProcessingCoordinator;
    private final MutableConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final UniqueConversationSecret uniqueConversationSecret;

    @Autowired
    public PostMessageResource(
            ProcessingContextFactory processingContextFactory,
            MessageProcessingCoordinator messageProcessingCoordinator,
            MutableConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            UniqueConversationSecret uniqueConversationSecret) {
        this.processingContextFactory = processingContextFactory;
        this.messageProcessingCoordinator = messageProcessingCoordinator;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.uniqueConversationSecret = uniqueConversationSecret;
    }

    @ApiOperation(
        value = "Create a conversation",
        notes = "Create a conversation between participants on an ad",
        nickname = "createConversation",
        tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PostMapping("/users/{userId}/ads/{adId}/conversations")
    public CreateConversationResponse createConversation(
            @PathVariable("userId") String userId,
            @PathVariable("adId") String adId,
            @RequestBody CreateConversationRequest createConversationRequest) {
        Set<String> participantIds = createConversationRequest.participantIds;
        if (createConversationRequest.participantIds.size() != 2) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "Conversation should have two participants");
        }
        if (!participantIds.contains(userId)) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "User is not a participant");
        }
        try (Timer.Context ignored = createConversationTimer.time()) {

            String buyerId = userId;
            String sellerId = participantIds.stream().filter(id -> !userId.equals(id)).findFirst().get();

            Optional<MutableConversation> existingConversation = conversationRepository.findExistingConversationFor(new ConversationIndexKey(buyerId, sellerId, adId));
            if (existingConversation.isPresent()) {
                return new CreateConversationResponse(true, existingConversation.get().getId());
            }

            HashMap<String, String> customValues = new HashMap<>();
            customValues.put(userIdentifierService.getBuyerUserIdName(), buyerId);
            customValues.put(userIdentifierService.getSellerUserIdName(), sellerId);

            NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(Guids.next()).
                    withAdId(adId).
                    withBuyer(buyerId, uniqueConversationSecret.nextSecret()).
                    withSeller(sellerId, uniqueConversationSecret.nextSecret()).
                    withCustomValues(customValues).
                    build();

            ConversationCreatedEvent conversationCreatedEvent = new ConversationCreatedEvent(newConversationBuilderCommand);

            String conversationId = newConversationBuilderCommand.getConversationId();
            conversationRepository.commit(conversationId, Arrays.asList(conversationCreatedEvent));

            return new CreateConversationResponse(false, conversationId);
        }
    }

    @ApiOperation(
        value = "Post a message",
        notes = "Post a message to an existing conversation",
        nickname = "postMessage",
        tags = "Conversations")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 404, message = "Conversation Not Found", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PostMapping("/users/{userId}/conversations/{conversationId}")
    public PostMessageResponse postMessage(
            @PathVariable("userId") String userId,
            @PathVariable("conversationId") String conversationId,
            @RequestBody PostMessageRequest postMessageRequest) {
        try (Timer.Context ignored = postMessageTimer.time()) {

            MutableConversation conversation = conversationRepository.getById(conversationId);

            if (conversation == null) {
                throw new ClientException(HttpStatus.NOT_FOUND, String.format("Conversation not found for ID: %s", conversationId));
            }

            MessageProcessingContext context = processingContextFactory.newContext(null, Guids.next());
            context.setConversation(conversation);
            if (userId.equals(conversation.getSellerId())) {
                context.setMessageDirection(MessageDirection.SELLER_TO_BUYER);
            } else if (userId.equals(conversation.getBuyerId())) {
                context.setMessageDirection(MessageDirection.BUYER_TO_SELLER);
            } else {
                throw new ClientException(HttpStatus.BAD_REQUEST, "User is not a participant");
            }

            AddMessageCommand addMessageCommand =
                    anAddMessageCommand(conversation.getId(), context.getMessageId())
                            .withMessageDirection(context.getMessageDirection())
                            .withSenderMessageIdHeader(context.getMessageId())
                            //.withInResponseToMessageId() TODO akobiakov: find out what this is used for
                            .withHeaders(Collections.emptyMap())
                            .withTextParts(Collections.singletonList(postMessageRequest.message))
                            .withAttachmentFilenames(Collections.emptyList())
                            .build();

            context.addCommand(addMessageCommand);

            String messageId = messageProcessingCoordinator.handleContext(Optional.empty(), context);
            return new PostMessageResponse(messageId);
        }
    }
}
