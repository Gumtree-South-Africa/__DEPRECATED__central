package com.ecg.messagebox.resources;

import com.ecg.comaas.events.Conversation;
import com.ecg.comaas.protobuf.MessageOuterClass;
import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.MessageBoxRepository;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.requests.CreateConversationRequest;
import com.ecg.messagebox.resources.requests.PostMessageRequest;
import com.ecg.messagebox.resources.responses.CreateConversationResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.PostMessageResponse;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.util.ConversationEventConverter;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import io.swagger.annotations.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.api.util.Constants.INTERRUPTED_WARNING;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostMessageResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostMessageResource.class);

    private final MutableConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final UniqueConversationSecret uniqueConversationSecret;
    private final MessageBoxRepository postBoxRepository;
    private final QueueService queueService;
    private final ConversationEventService conversationEventService;
    private final AnonymizedMailConverter anonymizedMailConverter;
    // As of now, in the end of the year 2018, it worth mentioning that the following object deals with the
    // old style, replyts2 events, and has nothing to do with conversation_events of the new comaas architecture.
    private final ConversationEventListeners conversationEventListeners;

    @Value("${replyts.tenant.short:${replyts.tenant}}")
    private String shortTenant;

    @Autowired
    public PostMessageResource(
            MutableConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            UniqueConversationSecret uniqueConversationSecret,
            MessageBoxRepository postBoxRepository,
            QueueService queueService,
            ConversationEventService conversationEventService,
            AnonymizedMailConverter anonymizedMailConverter,
            ConversationEventListeners conversationEventListeners) {
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.uniqueConversationSecret = uniqueConversationSecret;
        this.postBoxRepository = postBoxRepository;
        this.queueService = queueService;
        this.conversationEventService = conversationEventService;
        this.anonymizedMailConverter = anonymizedMailConverter;
        this.conversationEventListeners = conversationEventListeners;
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
            @ApiParam(value = "Message DateTime") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  DateTime createdAt,
            @ApiParam(value = "Conversation payload", required = true)
            @RequestBody CreateConversationRequest createConversationRequest) {

        DateTime convCreatedAt = createdAt == null ? new DateTime(DateTimeZone.UTC) : createdAt;

        List<Participant> participants = createConversationRequest.participants;
        if (participants.isEmpty()) {
            throw new ClientException(HttpStatus.BAD_REQUEST, "Field 'participants' must not be empty");
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
        if (createConversationRequest.metadata != null) {
            customValues.putAll(createConversationRequest.metadata);
        }
        customValues.put(userIdentifierService.getBuyerUserIdName(), buyer.getUserId());
        customValues.put(userIdentifierService.getSellerUserIdName(), seller.getUserId());

        String buyerSecret = uniqueConversationSecret.nextSecret();
        String sellerSecret = uniqueConversationSecret.nextSecret();

        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(Guids.next())
                .withAdId(adId)
                .withBuyer(buyer.getEmail(), buyerSecret)
                .withSeller(seller.getEmail(), sellerSecret)
                .withCustomValues(customValues)
                .withCreatedAt(convCreatedAt)
                .build();

        String conversationId = newConversationBuilderCommand.getConversationId();

        ConversationMetadata conversationMetadata = new ConversationMetadata(
                convCreatedAt,
                createConversationRequest.subject,
                createConversationRequest.title,
                createConversationRequest.imageUrl
        );
        ConversationThread conversationThread = new ConversationThread(conversationId, adId, userId, Visibility.ACTIVE,
                MessageNotification.RECEIVE, participants, null, conversationMetadata);

        postBoxRepository.createEmptyConversation(buyer.getUserId(), conversationThread);
        postBoxRepository.createEmptyConversation(seller.getUserId(), conversationThread);

        String buyerCloakedEmailAddress = anonymizedMailConverter.toCloakedEmailAddress(buyerSecret, ConversationRole.Buyer, newConversationBuilderCommand.getCustomValues());
        String sellerCloakedEmailAddress = anonymizedMailConverter.toCloakedEmailAddress(sellerSecret, ConversationRole.Seller, newConversationBuilderCommand.getCustomValues());

        placeConversationCreatedEventOnQueue(adId, conversationId, customValues, participants, convCreatedAt, buyerCloakedEmailAddress, sellerCloakedEmailAddress);
        DefaultMutableConversation.create(newConversationBuilderCommand).commit(conversationRepository, conversationEventListeners);

        LOG.debug("Created conversation id: {}, createdAt: {}", conversationId, convCreatedAt);

        return new CreateConversationResponse(false, conversationId);
    }

    private void placeConversationCreatedEventOnQueue(String adId, String conversationId, Map<String, String> customValues, List<Participant> participants, DateTime creationDate, String buyerCloakedEmailAddress, String sellerCloakedEmailAddress) {
        try {
            conversationEventService.sendConversationCreatedEvent(shortTenant, adId, conversationId, customValues, toConversationEventParticipants(participants, buyerCloakedEmailAddress, sellerCloakedEmailAddress), creationDate);
        } catch (InterruptedException e) {
            // we are not sure whether the message is now received by kafka or not. The caller should retry.
            LOG.warn("Aborting POST because thread is interrupted. conversation id: {}." + INTERRUPTED_WARNING, conversationId);
            Thread.currentThread().interrupt();
            throw new ClientException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread was interrupted.");
        }
    }

    private Set<Conversation.Participant> toConversationEventParticipants(List<Participant> participants, String buyerCloakedEmailAddress, String sellerCloakedEmailAddress) {
        Set<Conversation.Participant> participantSet = new HashSet<>();
        for (Participant participant : participants) {
            String cloakedEmailAddress =
                    participant.getRole() == BUYER
                            ? buyerCloakedEmailAddress
                            : participant.getRole() == SELLER
                            ? sellerCloakedEmailAddress
                            : null;

            participantSet.add(ConversationEventConverter.createParticipant(participant.getUserId(), participant.getName(),
                    participant.getEmail(), Conversation.Participant.Role.valueOf(participant.getRole().name()), cloakedEmailAddress));
        }

        return participantSet;
    }

    @ApiOperation(
            value = "Post a message",
            notes = "Post a message to an existing conversation",
            nickname = "postMessage",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PostMapping(value = "/users/{userId}/conversations/{conversationId}", consumes = "application/json")
    public PostMessageResponse postMessage(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Message payload", required = true) @RequestBody PostMessageRequest payload,
            @ApiParam(value = "Message DateTime") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  DateTime createdAt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) {

        MessageBuilder messageBuilder = new MessageBuilder(correlationIdHeader, createdAt);
        messageBuilder = messageBuilder.setPayload(userId, conversationId, payload);
        MessageOuterClass.Message kafkaMessage = messageBuilder.build();

        placeMessageOnIncomingQueue(kafkaMessage);

        LOG.debug("Posted message for conversation id: {}, with message id {}, correlation id: {}, timestamp: {}",
                conversationId, kafkaMessage.getMessageId(), kafkaMessage.getCorrelationId(), kafkaMessage.getReceivedTime());

        return new PostMessageResponse(kafkaMessage.getMessageId());
    }


    @ApiOperation(
            value = "Post a message with attachment",
            notes = "Post a message with attachment to an existing conversation. It allows one attachment and the total size of the request (message + attachment) must not exceed 10 MBytes.",
            nickname = "postMessage",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Bad Request", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @PostMapping(value = "/users/{userId}/conversations/{conversationId}/attachment", consumes = "multipart/form-data")
    public PostMessageResponse postMessageWithAttachment(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Message payload and metadata", required = true) @RequestPart(value = "message") PostMessageRequest payload,
            @ApiParam(value = "Attachment", required = true) @RequestPart(value = "attachment") MultipartFile attachment,
            @ApiParam(value = "Message DateTime") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  DateTime createdAt,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) throws IOException {

        MessageBuilder messageBuilder = new MessageBuilder(correlationIdHeader, createdAt);
        messageBuilder = messageBuilder.setPayload(userId, conversationId, payload).setAttachment(attachment);
        MessageOuterClass.Message kafkaMessage = messageBuilder.build();

        placeMessageOnIncomingQueue(kafkaMessage);

        LOG.debug("Posted message for conversation id: {}, with message id {}, correlation id: {}, attachment name :{}, attachment size: {} bytes, timestamp: {}",
                conversationId, kafkaMessage.getMessageId(), kafkaMessage.getCorrelationId(), attachment.getOriginalFilename(), attachment.getSize(), kafkaMessage.getReceivedTime());

        return new PostMessageResponse(kafkaMessage.getMessageId());
    }

    /**
     * Post message (constructed with or without attachment)
     */
    private void placeMessageOnIncomingQueue(Message m) {
        try {
            queueService.publishSynchronously(KafkaTopicService.getTopicIncoming(shortTenant), m);
        } catch (InterruptedException e) {
            // we are not sure whether the message is now received by kafka or not. The caller should retry.
            LOG.warn("Aborting post message call because thread is interrupted. correlation id: {}. " + INTERRUPTED_WARNING, m.getCorrelationId());
            Thread.currentThread().interrupt();
            throw new ClientException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread was interrupted.");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return ex.getMessage();
    }


}
