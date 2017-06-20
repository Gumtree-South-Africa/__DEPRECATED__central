package com.ecg.de.mobile.replyts.sms;

public class SmsSendResponse {

    private String id;
    private String msg;

    public SmsSendResponse(String id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public String getId() {
    
        return id;
    }

    public String getMsg() {
        return msg;
    }
}
