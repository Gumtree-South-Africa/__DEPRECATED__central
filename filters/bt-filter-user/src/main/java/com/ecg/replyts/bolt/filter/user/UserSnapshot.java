package com.ecg.replyts.bolt.filter.user;

import java.util.Date;

public class UserSnapshot {
    private String userEmail;

    private String userStatus;

    private String userStateDetail;

    private Date creationDate;

    private Date registrationDate;

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getUserStateDetail() {
        return userStateDetail;
    }

    public void setUserStateDetail(String userStateDetail) {
        this.userStateDetail = userStateDetail;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }
}
