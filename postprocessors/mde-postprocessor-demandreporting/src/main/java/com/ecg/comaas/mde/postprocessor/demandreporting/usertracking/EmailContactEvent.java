package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;


import static java.util.Objects.requireNonNull;

class EmailContactEvent extends TrackingEvent {

    final Vi vi;
    final ClientInfo ci;
    final EmailMessage msg;

    private EmailContactEvent(Builder b) {
        super(b.head);
        this.vi = b.vi;
        this.msg = b.msg;
        this.ci = b.ci;
    }

    static class Builder {

        private Head head;
        private EmailMessage msg;
        private Vi vi;
        private ClientInfo ci;
        private String txId;
        private String txSeq;


        Builder txId(String v) {
            txId = v;
            return this;
        }
        Builder ci(ClientInfo ci) {
            this.ci = ci;
            return this;
        }

        Builder txSeq(String v) {
            txSeq = v;
            return this;
        }

        Builder vi(Vi v) {
            vi = v;
            return this;
        }

        Builder message(EmailMessage m) {
            msg = m;
            return this;
        }

        EmailContactEvent build() {
            requireNonNull(vi);
            head = Head.builder()
                .ns("ad_contact_events.EmailContact")
                .txId(txId)
                .txSeq(txSeq)
                .build();

            return new EmailContactEvent(this);
        }
    }


    static Builder builder() {
        return new Builder();
    }


}
