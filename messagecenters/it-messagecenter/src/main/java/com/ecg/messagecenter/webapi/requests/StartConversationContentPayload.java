package com.ecg.messagecenter.webapi.requests;

/**
 * Created by jaludden on 30/11/15.
 */
public class StartConversationContentPayload {

    private String buyerName;
    private String sellerName;
    private String sellerEmail;
    private long adId;
    private String message;
    private String adTitle;

    public StartConversationContentPayload() {
    }

    public void setAdId(long adId) {
        this.adId = adId;
    }

    public long getAdId() {
        return adId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public String getSubject() {
        return "Risposta a \"" + getAdTitle() + "\"";
    }

    public String getGreating() {
        return "Ecco il suo messaggio:";
    }

    public String getType() {
        return "annuncio";
    }

    public void cleanupMessage() {
        if (message.trim().startsWith(">")) {
            message = message.trim().substring(1);
        }
        message = RequestUtil.cleanText(message);
    }
}
