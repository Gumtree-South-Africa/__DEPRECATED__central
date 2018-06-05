package com.ecg.messagecenter.kjca.pushmessage;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;

public abstract class PushService {

    private static final Counter SEND_COUNTER_PUSH_SENT = TimingReports.newCounter("message-box.send.push-message-sent");
    private static final Counter SEND_COUNTER_PUSH_NO_DEVICE = TimingReports.newCounter("message-box.send.push-message-no-device");
    private static final Counter SEND_COUNTER_PUSH_FAILED = TimingReports.newCounter("message-box.send.push-message-failed");

    public abstract Result sendPushMessage(final PushMessagePayload payload);

    public void incrementCounter(Result.Status status) {
        if (PushService.Result.Status.OK == status) {
            SEND_COUNTER_PUSH_SENT.inc();
        }

        if (PushService.Result.Status.NOT_FOUND == status) {
            SEND_COUNTER_PUSH_NO_DEVICE.inc();
        }

        if (PushService.Result.Status.ERROR == status) {
            SEND_COUNTER_PUSH_FAILED.inc();
        }
    }

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
