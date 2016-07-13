package com.ecg.de.mobile.replyts.demand.usertracking;


class EmailMessage {
    final AdRef ad;
    final String ip;
    final String senderMailAddress;
    final String receiverMailAddress;
    final String replyToMailAddress;
    final String subject;
    final String plainText;

    private EmailMessage(Builder b) {
        ad = b.ad;
        ip = b.ip;
        senderMailAddress = b.senderMailAddress;
        receiverMailAddress = b.receiverMailAddress;
        replyToMailAddress = b.replyToMailAddress;
        subject = b.subject;
        plainText = b.plainText;
    }

    static class Builder {
        AdRef ad;
        String ip;
        String senderMailAddress;
        String receiverMailAddress;
        String replyToMailAddress;
        String subject;
        String plainText;

        Builder ad(AdRef a) {
            ad = a;
            return this;
        }

        Builder ip(String v) {
            ip = v;
            return this;
        }

        Builder senderMailAddress(String v) {
            senderMailAddress = v;
            return this;
        }

        Builder receiverMailAddress(String v) {
            receiverMailAddress = v;
            return this;
        }

        Builder replyToMailAddress(String v) {
            replyToMailAddress = v;
            return this;
        }

        Builder subject(String v) {
            subject = v;
            return this;
        }

        Builder plainText(String v) {
            plainText = v;
            return this;
        }

        EmailMessage build() {
            return new EmailMessage(this);
        }
    }

    static Builder builder() {
        return new Builder();
    }
}
