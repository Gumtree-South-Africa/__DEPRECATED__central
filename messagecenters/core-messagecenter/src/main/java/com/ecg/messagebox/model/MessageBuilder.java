package com.ecg.messagebox.model;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.comaas.protobuf.MessageOuterClass;
import com.ecg.messagebox.resources.requests.PostMessageRequest;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.joda.time.DateTime;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public class MessageBuilder {
    private final MessageOuterClass.Message.Builder builder;

    public MessageBuilder(String correlationId, DateTime datetime) {

        Instant createdAt = datetime == null ? Instant.now() : Instant.ofEpochMilli(datetime.getMillis());
        String ci = correlationId == null ? UUID.randomUUID().toString() : correlationId;

        this.builder = MessageOuterClass.Message
                .newBuilder()
                .setMessageId(UUIDs.timeBased().toString())
                .setReceivedTime(Timestamp
                        .newBuilder()
                        .setSeconds(createdAt.getEpochSecond())
                        .setNanos(createdAt.getNano())
                        .build())
                .setCorrelationId(ci);
    }

    public MessageBuilder setPayload(String userId, String conversationId, PostMessageRequest postMessageRequest) {
        builder.setPayload(MessageOuterClass.Payload
                .newBuilder()
                .setConversationId(conversationId)
                .setUserId(userId)
                .setMessage(postMessageRequest.message)
                .build())
                .putAllMetadata(postMessageRequest.metadata);
        return this;
    }

    public MessageBuilder setAttachment(MultipartFile attachment) throws IOException {

        if (attachment != null) {
            builder.addAttachments(MessageOuterClass.Attachment.newBuilder()
                    .setFileName(attachment.getOriginalFilename())
                    .setBody(ByteString.readFrom(attachment.getInputStream())));
        }
        return this;
    }

    public MessageOuterClass.Message build() {
        return builder.build();
    }
}
