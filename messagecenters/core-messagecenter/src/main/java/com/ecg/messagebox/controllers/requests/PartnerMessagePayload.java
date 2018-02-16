package com.ecg.messagebox.controllers.requests;

import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class PartnerMessagePayload {

    private String adId;
    private String adTitle;
    private String subject;
    private Participant buyer;
    private Participant seller;
    private String text;
    private String senderUserId;
    private MessageType type;

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Participant getBuyer() {
        return buyer;
    }

    public void setBuyer(Participant buyer) {
        this.buyer = buyer;
    }

    public Participant getSeller() {
        return seller;
    }

    public void setSeller(Participant seller) {
        this.seller = seller;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartnerMessagePayload that = (PartnerMessagePayload) o;
        return Objects.equal(adId, that.adId) &&
                Objects.equal(adTitle, that.adTitle) &&
                Objects.equal(subject, that.subject) &&
                Objects.equal(buyer, that.buyer) &&
                Objects.equal(seller, that.seller) &&
                Objects.equal(text, that.text) &&
                Objects.equal(senderUserId, that.senderUserId) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(adId, adTitle, subject, buyer, seller, text, senderUserId, type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("adId", adId)
                .add("adTitle", adTitle)
                .add("subject", subject)
                .add("buyer", buyer)
                .add("seller", seller)
                .add("text", text)
                .add("senderUserId", senderUserId)
                .add("type", type)
                .toString();
    }
}
