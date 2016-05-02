package com.ecg.de.ebayk.messagecenter.pushmessage;

public abstract class PushService {
    public static final String PUSH_DELIVERY_CHANNEL = "push";

    public abstract Result sendPushMessage(final PushMessagePayload payload);

    public static class Result {

        public enum Status {
            OK, NOT_FOUND, ERROR
        }

        private PushMessagePayload payload;
        private Result.Status status;
        private Exception e;

        private Result(PushMessagePayload payload, Status status, Exception e) {
            this.payload = payload;
            this.status = status;
            this.e = e;
        }

        private Result(PushMessagePayload payload, Status status) {
            this.payload = payload;
            this.status = status;
            this.e = null;
        }

        public static Result ok(PushMessagePayload payload) {
            return new Result(payload, Result.Status.OK);
        }

        public static Result notFound(PushMessagePayload payload) {
            return new Result(payload, Result.Status.NOT_FOUND);
        }

        public static Result error(PushMessagePayload payload, Exception e) {
            return new Result(payload, Result.Status.ERROR, e);
        }

        public Result.Status getStatus() {
            return status;
        }

        public PushMessagePayload getPayload() {
            return payload;
        }

        public Exception getException() {
            return e;
        }
    }
}
