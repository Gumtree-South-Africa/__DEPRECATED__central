package com.ecg.comaas.mde.postprocessor.sms;

public class SmsSendRequest {
    
    private String phoneNumber;
    private String message;
    private String purpose;

    public SmsSendRequest(String phoneNumber, String message, String purpose) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.purpose = purpose;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public String getPurpose() {
        return purpose;
    }
}
