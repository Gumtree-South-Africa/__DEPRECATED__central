package com.ecg.de.mobile.replyts.sms;

public class SmsSendRequest {
    
    private String phoneNumber;
    private String message;

    public SmsSendRequest(String phoneNumber, String message) {
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }
}
