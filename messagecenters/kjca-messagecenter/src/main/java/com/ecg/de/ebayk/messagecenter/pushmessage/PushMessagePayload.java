package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.google.common.collect.Maps;
import net.sf.json.JSONObject;

import java.util.Map;
import java.util.Optional;

public class PushMessagePayload {

    private final String email;
    private final String userId;
    private final String message;
    private final String activity;
    private final Optional<Integer> alertCounter;
    private final Optional<Map<String, String>> details;

    public PushMessagePayload(String email, String userId, String message, String activity, Optional<Map<String, String>> details, Optional<Integer> alertCounter) {
        this.email = email;
        this.userId = userId;
        this.message = message;
        this.activity = activity;
        this.details = details;
        this.alertCounter = alertCounter;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public Optional<Map<String, String>> getDetails() {
        return details;
    }

    public String getUserId() {
        return userId;
    }

    public String getActivity() {
        return activity;
    }

    public Optional<Integer> getAlertCounter() {
        return alertCounter;
    }

    public String asJson() {
        JSONObject json = new JSONObject();

        json.put("email", email);
        json.put("userId", userId);
        json.put("message", message);
        json.put("activity", activity);
        json.put("details", details.isPresent() ? details.get() : Maps.newHashMap());
        if (alertCounter.isPresent()) {
            json.put("alertCounter", alertCounter.get());
        }

        return json.toString();
    }

}
