package com.ecg.messagebox.resources;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.comaas.protobuf.MessageOuterClass.Payload;
import com.ecg.messagebox.controllers.requests.CreateConversationRequest;
import com.ecg.messagebox.controllers.requests.PostMessageRequest;
import com.ecg.messagebox.controllers.responses.CreateConversationResponse;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.PostMessageResponse;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.cluster.XidFactory;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.google.protobuf.Timestamp;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static org.joda.time.DateTime.now;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PostMessageResource {

    private static final Logger LOG = LoggerFactory.getLogger(PostMessageResource.class);

    private final ProcessingContextFactory processingContextFactory;
    private final MutableConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final UniqueConversationSecret uniqueConversationSecret;
    private final CassandraPostBoxRepository postBoxRepository;
    private final QueueService queueService;

    @Value("${replyts.tenant.short:${replyts.tenant}}")
    private String shortTenant;

    @Autowired
    public PostMessageResource(
            ProcessingContextFactory processingContextFactory,
            MutableConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            UniqueConversationSecret uniqueConversationSecret,
            CassandraPostBoxRepository postBoxRepository,
            QueueService queueService) {
        this.processingContextFactory = processingContextFactory;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.uniqueConversationSecret = uniqueConversationSecret;
        this.postBoxRepository = postBoxRepository;
        this.queueService = queueService;
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
        if (createConversationRequest.metadata != null) {
            customValues.putAll(createConversationRequest.metadata);
        }
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

        postBoxRepository.createEmptyConversation(buyer.getUserId(), conversationThread);
        postBoxRepository.createEmptyConversation(seller.getUserId(), conversationThread);

        return new CreateConversationResponse(false, conversationId);
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
    @PostMapping("/users/{userId}/conversations/{conversationId}")
    public PostMessageResponse postMessage(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Message payload", required = true) @RequestBody PostMessageRequest postMessageRequest,
            @RequestHeader("X-Correlation-ID") Optional<String> correlationId) {
        Instant now = Instant.now();
        Message retryableMessage = Message
                .newBuilder()
                .setReceivedTime(Timestamp
                        .newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setCorrelationId(correlationId.orElseGet(XidFactory::nextXid))
                .setPayload(Payload.newBuilder()
                        .setConversationId(conversationId)
                        .setUserId(userId)
                        .setMessage(postMessageRequest.message)
                        .build())
                .putAllMetadata(postMessageRequest.metadata == null ? Collections.emptyMap() : postMessageRequest.metadata)
                .build();

        queueService.publish(KafkaTopicService.getTopicIncoming(shortTenant), retryableMessage);
        return new PostMessageResponse(retryableMessage.getCorrelationId());
    }
}
