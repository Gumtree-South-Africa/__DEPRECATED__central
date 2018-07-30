package com.ecg.messagebox.resources;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.comaas.protobuf.MessageOuterClass;
import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.comaas.protobuf.MessageOuterClass.Payload;
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
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    private static final String X_MESSAGE_ID_HEADER = "X-Message-ID";

    private final MutableConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final UniqueConversationSecret uniqueConversationSecret;
    private final CassandraPostBoxRepository postBoxRepository;
    private final QueueService queueService;

    @Value("${replyts.tenant.short:${replyts.tenant}}")
    private String shortTenant;

    @Autowired
    public PostMessageResource(
            MutableConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            UniqueConversationSecret uniqueConversationSecret,
            CassandraPostBoxRepository postBoxRepository,
            QueueService queueService) {
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

        String title = createConversationRequest.title;
        String imageUrl = createConversationRequest.imageUrl;

        Map<String, String> customValues = new HashMap<>();
        if (createConversationRequest.metadata != null) {
            customValues.putAll(createConversationRequest.metadata);
        }
        customValues.put(userIdentifierService.getBuyerUserIdName(), buyer.getUserId());
        customValues.put(userIdentifierService.getSellerUserIdName(), seller.getUserId());

        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(Guids.next())
                .withAdId(adId)
                .withBuyer(buyer.getEmail(), uniqueConversationSecret.nextSecret())
                .withSeller(seller.getEmail(), uniqueConversationSecret.nextSecret())
                .withCustomValues(customValues)
                .build();

        ConversationCreatedEvent conversationCreatedEvent = new ConversationCreatedEvent(newConversationBuilderCommand);

        String conversationId = newConversationBuilderCommand.getConversationId();
        conversationRepository.commit(conversationId, Collections.singletonList(conversationCreatedEvent));

        ConversationMetadata conversationMetadata = new ConversationMetadata(now(), subject, title, imageUrl);
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
    @PostMapping(value = "/users/{userId}/conversations/{conversationId}", consumes = "application/json")
    public PostMessageResponse postMessage(
            @ApiParam(value = "User ID", required = true) @PathVariable("userId") String userId,
            @ApiParam(value = "Conversation ID", required = true) @PathVariable("conversationId") String conversationId,
            @ApiParam(value = "Message payload", required = true) @RequestBody PostMessageRequest postMessageRequest,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) {
        Message.Builder kafkaMessage = buildMessage(userId, conversationId, postMessageRequest, correlationIdHeader);
        queueService.publish(KafkaTopicService.getTopicIncoming(shortTenant), kafkaMessage.build());
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
            @ApiParam(value = "Message payload and metadata", required = true) @RequestPart (value = "message") PostMessageRequest postMessageRequest,
            @ApiParam(value = "Attachment", required = true) @RequestPart(value = "attachment") MultipartFile attachment,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationIdHeader) throws IOException {

        Message.Builder kafkaMessage = buildMessage(userId, conversationId, postMessageRequest, correlationIdHeader);

        kafkaMessage
                .addAttachments(MessageOuterClass.Attachment
                        .newBuilder()
                        .setFileName(attachment.getOriginalFilename())
                        .setBody(ByteString.readFrom(attachment.getInputStream()))
                );

        queueService.publish(KafkaTopicService.getTopicIncoming(shortTenant), kafkaMessage.build());
        LOG.debug("Posted message for conversation id: {}, with message id {}, correlation id: {}, attachment name :{}, attachment size: {} bytes",
                conversationId, kafkaMessage.getMessageId(), kafkaMessage.getCorrelationId(), attachment.getOriginalFilename(), attachment.getSize());

        return new PostMessageResponse(kafkaMessage.getMessageId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return ex.getMessage();
    }

    private Message.Builder buildMessage(String userId, String conversationId, PostMessageRequest postMessageRequest, String correlationIdHeader) {
        Instant now = Instant.now();
        String correlationId = correlationIdHeader != null ? correlationIdHeader : XidFactory.nextXid();

        // This is undocumented behaviour which is set to be removed in COMAAS-1226
        // At the moment we can't ignore X-Message-Id entirely and just write/read the messageId field of the
        // kafka payload, because the emails originating from MP (the tenant itself) rely on this header to
        // render the chat ui correctly. So we can only get rid of the edge case when MP is fully on the post message api.
        // (sorry).
        Map<String, String> caseInsensitiveMetaValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveMetaValues.putAll(postMessageRequest.metadata);
        String messageId = caseInsensitiveMetaValues.computeIfAbsent(X_MESSAGE_ID_HEADER, k -> UUIDs.timeBased().toString());

        return Message
                .newBuilder()
                .setMessageId(messageId)
                .setReceivedTime(Timestamp
                        .newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .setCorrelationId(correlationId)
                .setPayload(Payload
                    .newBuilder()
                    .setConversationId(conversationId)
                    .setUserId(userId)
                    .setMessage(postMessageRequest.message)
                    .build())
                .putAllMetadata(caseInsensitiveMetaValues);
    }
}
