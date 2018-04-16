package com.ecg.au.gumtree.messagelogging;

public class MySqlTestProjection {

    private final String buyerEmail;
    private final String sellerEmail;
    private final String adId;

    public MySqlTestProjection(String buyerEmail, String sellerEmail, String adId) {
        this.buyerEmail = buyerEmail;
        this.sellerEmail = sellerEmail;
        this.adId = adId;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public String getAdId() {
        return adId;
    }
}
