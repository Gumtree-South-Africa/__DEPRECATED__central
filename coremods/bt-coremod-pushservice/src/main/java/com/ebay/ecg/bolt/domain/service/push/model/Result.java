package com.ebay.ecg.bolt.domain.service.push.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Optional;

public class Result {
    @JsonSerialize
    @JsonDeserialize
    public enum Status {
        OK, NOT_FOUND, ERROR
    }
    
    @JsonSerialize
    @JsonDeserialize
    private String deviceToken;

    @JsonSerialize
    @JsonDeserialize
    private PushMessagePayload payload;
    
    @JsonSerialize
    @JsonDeserialize
    private Result.Status status;
    
    @JsonSerialize
    @JsonDeserialize
    private Optional<Exception> e;

    private Result(PushMessagePayload payload, Result.Status status, String deviceToken, Optional<Exception> e) {
        this.payload = payload;
        this.status = status;
        this.deviceToken = deviceToken;
        this.e = e;
    }

    public static Result ok(PushMessagePayload payload, String deviceToken) {
        return new Result(payload, Result.Status.OK, deviceToken, Optional.empty());
    }

    public static Result notFound(PushMessagePayload payload, String deviceToken) {
        return new Result(payload, Result.Status.NOT_FOUND, deviceToken, Optional.empty());
    }

    public static Result error(PushMessagePayload payload, String deviceToken, Exception e) {
        return new Result(payload, Result.Status.ERROR, deviceToken, Optional.of(e));
    }

    public Result.Status getStatus() {
        return status;
    }

    public PushMessagePayload getPayload() {
        return payload;
    }   
    
    public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}

	public Optional<Exception> getException() {
        return e;
    }
}