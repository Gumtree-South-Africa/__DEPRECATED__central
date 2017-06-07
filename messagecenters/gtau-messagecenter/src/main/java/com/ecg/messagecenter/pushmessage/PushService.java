package com.ecg.messagecenter.pushmessage;

import java.util.Optional;

public abstract class PushService {
    public static final String PUSH_DELIVERY_CHANNEL = "push";

    public abstract Result sendPushMessage(final PushMessagePayload payload);

    public static class Result {
        public enum Status {
            OK, NOT_FOUND, ERROR
        }

        private PushMessagePayload payload;
        private KmobilePushService.Result.Status status;
        private Optional<Exception> e;

        private Result(PushMessagePayload payload, KmobilePushService.Result.Status status, Optional<Exception> e) {
            this.payload = payload;
            this.status = status;
            this.e = e;
        }

        public static Result ok(PushMessagePayload payload) {
            return new Result(payload, KmobilePushService.Result.Status.OK, Optional.<Exception>empty());
        }

        public static Result notFound(PushMessagePayload payload) {
            return new Result(payload, KmobilePushService.Result.Status.NOT_FOUND, Optional.<Exception>empty());
        }

        public static Result error(PushMessagePayload payload, Exception e) {
            return new Result(payload, KmobilePushService.Result.Status.ERROR, Optional.of(e));
        }

        public KmobilePushService.Result.Status getStatus() {
            return status;
        }

        public PushMessagePayload getPayload() {
            return payload;
        }

        public Optional<Exception> getException() {
            return e;
        }
    }
}