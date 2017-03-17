package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.google.common.base.Optional;

/**
 * @author mdarapour
 */
public  abstract class PushService {
    public  abstract Result sendPushMessage(final PushMessagePayload payload);

    public static final class Result {

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
            return new Result(payload, KmobilePushService.Result.Status.OK, Optional.<Exception>absent());
        }

        public static Result notFound(PushMessagePayload payload) {
            return new Result(payload, KmobilePushService.Result.Status.NOT_FOUND, Optional.<Exception>absent());
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
