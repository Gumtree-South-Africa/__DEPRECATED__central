package com.ecg.comaas.mde.listener.rating;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

public class EmailInviteEntity {

    private static final String EMAIL_HASH_PREFIX = "1fde573aa6";

    private String source;

    private String buyerEmail;

    private long dealerId;

    private long adId;

    private String ipAddress;

    private String locale;

    private String replytsConversationId;

    private String mobileViId;

    private String buyerDeviceId;

    private String buyerCustomerId;

    private String triggerType;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public long getDealerId() {
        return dealerId;
    }

    public void setDealerId(long dealerId) {
        this.dealerId = dealerId;
    }

    public long getAdId() {
        return adId;
    }

    public void setAdId(long adId) {
        this.adId = adId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getReplytsConversationId() {
        return replytsConversationId;
    }

    public void setReplytsConversationId(String replytsConversationId) {
        this.replytsConversationId = replytsConversationId;
    }

    public String getMobileViId() {
        return mobileViId;
    }

    public void setMobileViId(String mobileViId) {
        this.mobileViId = mobileViId;
    }

    public String getBuyerDeviceId() {
        return buyerDeviceId;
    }

    public void setBuyerDeviceId(String buyerDeviceId) {
        this.buyerDeviceId = buyerDeviceId;
    }

    public String getBuyerCustomerId() {
        return buyerCustomerId;
    }

    public void setBuyerCustomerId(String buyerCustomerId) {
        this.buyerCustomerId = buyerCustomerId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getBuyerEmailHash() {
        CharSequence input = (buyerEmail == null) ? EMAIL_HASH_PREFIX : EMAIL_HASH_PREFIX + buyerEmail;
        return Hashing.sha1().hashString(input, StandardCharsets.UTF_8).toString();
    }
}