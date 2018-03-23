package com.ecg.messagebox.resources;

import com.ecg.messagebox.controllers.requests.CreateConversationRequest;
import com.ecg.messagebox.controllers.requests.PostMessageRequest;
import com.ecg.messagebox.controllers.responses.CreateConversationResponse;
import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
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
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static org.joda.time.DateTime.now;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostMessageResource {

    private final ProcessingContextFactory processingContextFactory;
    private final MessageProcessingCoordinator messageProcessingCoordinator;
    private final MutableConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final UniqueConversationSecret uniqueConversationSecret;
    private final CassandraPostBoxRepository postBoxRepository;

    @Autowired
    public PostMessageResource(
            ProcessingContextFactory processingContextFactory,
            MessageProcessingCoordinator messageProcessingCoordinator,
            MutableConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            UniqueConversationSecret uniqueConversationSecret,
            CassandraPostBoxRepository postBoxRepository) {
        this.processingContextFactory = processingContextFactory;
        this.messageProcessingCoordinator = messageProcessingCoordinator;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.uniqueConversationSecret = uniqueConversationSecret;
        this.postBoxRepository = postBoxRepository;
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
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Ad ID", required = true) @PathVariable("adId") String adId,
            @ApiParam(value = "Conversation payload", required = true) @RequestBody CreateConversationRequest createConversationRequest) {
        List<Participant> participants = createConversationRequest.participants;
        if (participants == null || participants.isEmpty()) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "Field 'participants' must not be empty");
        }
        String subject = createConversationRequest.subject;
        if (subject == null || subject.trim().isEmpty()) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "Field 'subject' must not be empty");
        }
        if (participants.stream().noneMatch(participant -> participant.getUserId().equals(userId))) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "User ID should be one of the participants");
        }
        Participant buyer = participants.stream().filter(participant -> participant.getRole() == BUYER).findFirst()
                .orElseThrow(() -> new ClientException(HttpStatus.BAD_REQUEST, "Participants should contain a buyer role"));
        Participant seller = participants.stream().filter(participant -> participant.getRole() == SELLER).findFirst()
                .orElseThrow(() -> new ClientException(HttpStatus.BAD_REQUEST, "Participants should contain a seller role"));

        Optional<MutableConversation> existingConversation = conversationRepository.findExistingConversationFor(new ConversationIndexKey(buyer.getUserId(), seller.getUserId(), adId));
        if (existingConversation.isPresent()) {
            return new CreateConversationResponse(true, existingConversation.get().getId());
        }

        Map<String, String> customValues = new HashMap<>();
        customValues.put(userIdentifierService.getBuyerUserIdName(), buyer.getUserId());
        customValues.put(userIdentifierService.getSellerUserIdName(), seller.getUserId());

        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(Guids.next())
                .withAdId(adId)
                .withBuyer(buyer.getUserId(), uniqueConversationSecret.nextSecret())
                .withSeller(seller.getUserId(), uniqueConversationSecret.nextSecret())
                .withCustomValues(customValues)
                .build();

        ConversationCreatedEvent conversationCreatedEvent = new ConversationCreatedEvent(newConversationBuilderCommand);

        String conversationId = newConversationBuilderCommand.getConversationId();
        conversationRepository.commit(conversationId, Collections.singletonList(conversationCreatedEvent));

        ConversationMetadata conversationMetadata = new ConversationMetadata(now(), subject, null, null);
        ConversationThread conversationThread = new ConversationThread(conversationId, adId, userId, Visibility.ACTIVE,
                MessageNotification.RECEIVE, participants, null, conversationMetadata);

        postBoxRepository.createEmptyConversation(userId, conversationThread);

        return new CreateConversationResponse(false, conversationId);
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
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Message payload", required = true) @RequestBody PostMessageRequest postMessageRequest) {

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
