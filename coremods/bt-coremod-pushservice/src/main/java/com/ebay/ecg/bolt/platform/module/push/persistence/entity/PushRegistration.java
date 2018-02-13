package com.ebay.ecg.bolt.platform.module.push.persistence.entity;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndexes(@CompoundIndex(
  name = "push_registration_primary_idx",
  def = "{'registerUserId':1, 'notificationType': 1, 'pushProvider':1, 'locale':1, 'appType':1}",
  unique = true))
public class PushRegistration {
    @Id
	private String id;

	private String registerUserId;

    private String notificationType;

    private String pushProvider;

	private String appType;

    private Locale locale;

    private List<String> deviceTokens;

    private List<PwaDetails> pwaDetails;

    private Date creationDate;

    private Date modificationDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegisterUserId() {
        return registerUserId;
    }

    public void setRegisterUserId(String registerUserId) {
        this.registerUserId = registerUserId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getPushProvider() {
        return pushProvider;
    }

    public void setPushProvider(String pushProvider) {
        this.pushProvider = pushProvider;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public List<String> getDeviceTokens() {
        return deviceTokens;
    }

    public void setDeviceTokens(List<String> deviceTokens) {
        this.deviceTokens = deviceTokens;
    }

    public List<PwaDetails> getPwaDetails() {
        return pwaDetails;
    }

    public void setPwaDetails(List<PwaDetails> pwaDetails) {
        this.pwaDetails = pwaDetails;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }
}