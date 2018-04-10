package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;

import static com.ecg.comaas.mde.postprocessor.demandreporting.Utils.intValueOr;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Clock.systemUTC;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Base class for all tracking events.
 */
class TrackingEvent {

    final Head head;

    TrackingEvent(Head head) {
        this.head = head;
    }

    static class Head {

        final String app;
        final String id;
        final String ns;
        final String time;
        final String txId;
        final int txSeq;

        private Head(Builder b) {
            this.app = "replyts";
            this.id = b.id;
            this.time = b.time;
            this.ns = b.ns;
            this.txId = b.txId;
            this.txSeq = b.txSeq;
        }

        static Clock clock = systemUTC();

        final static class Builder {

            private String time;
            private String txId;
            private int txSeq = 0;
            private String ns;
            private String id;

            Builder txId(String value) {
                this.txId = isNullOrEmpty(value) ? UUID.randomUUID().toString() : value;
                return this;
            }

            Builder txSeq(@Nullable String s) {
                this.txSeq = intValueOr(s, 0);
                return this;
            }

            Builder ns(String v) {
                this.ns = v;
                return this;
            }

            Head build() {
                requireNonNull(ns);
                requireNonNull(txId);
                id = UUID.randomUUID().toString();
                time = ZonedDateTime.now(clock).format(ISO_INSTANT);

                return new Head(this);
            }
        }

        static Builder builder() {
            return new Builder();
        }
    }
}
