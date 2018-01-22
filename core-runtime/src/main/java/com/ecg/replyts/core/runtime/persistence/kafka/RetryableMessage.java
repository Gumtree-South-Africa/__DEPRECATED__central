package com.ecg.replyts.core.runtime.persistence.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class RetryableMessage {
    private final Instant messageReceivedTime;
    private final Instant nextConsumptionTime;
    private final byte[] payload;
    private final int triedCount;
    private final String correlationId;

    @JsonCreator
    public RetryableMessage(
            @JsonProperty("messageReceivedTime") Instant messageReceivedTime,
            @JsonProperty("nextConsumptionTime") Instant nextConsumptionTime,
            @JsonProperty("payload") byte[] payload,
            @JsonProperty("triedCount") int triedCount,
            @JsonProperty("correlationId") String correlationId) {
        this.messageReceivedTime = messageReceivedTime;
        this.nextConsumptionTime = nextConsumptionTime;
        this.payload = payload;
        this.triedCount = triedCount;
        this.correlationId = correlationId;
    }

    public Instant getMessageReceivedTime() {
        return messageReceivedTime;
    }

    public Instant getNextConsumptionTime() {
        return nextConsumptionTime;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getTriedCount() {
        return triedCount;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
