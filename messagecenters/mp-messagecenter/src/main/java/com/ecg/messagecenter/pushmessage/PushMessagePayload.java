package com.ecg.messagecenter.pushmessage;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import net.sf.json.JSONObject;

import java.util.Map;

public class PushMessagePayload {

    private final String userId;
    private final String message;
    private final String activity;
    private final Optional<Long> alertCounter;
    private final Map<String, String> details;
    private final Optional<Map<String, String>> gcmDetails;
    private final Optional<Map<String, String>> apnsDetails;

    public PushMessagePayload(String userId, String message, String activity, Map<String, String> details) {
        this(userId, message, activity, details, Optional.<Long>absent(), Optional.<Map<String, String>>absent(), Optional.<Map<String, String>>absent());
    }

    public PushMessagePayload(String userId, String message, String activity, Map<String, String> details, Optional<Long> alertCounter, Optional<Map<String, String>> gcmDetails, Optional<Map<String, String>> apnsDetails) {
        if (details == null) {
            details = Maps.newHashMap();
        }
        this.gcmDetails = gcmDetails;
        this.apnsDetails = apnsDetails;
        this.userId = userId;
        this.message = message;
        this.activity = activity;
        this.details = details;
        this.alertCounter = alertCounter;
    }

    public String getUserId() {
        return userId;
    }

    public String getActivity() {
        return activity;
    }

    public Optional<Long> getAlertCounter() {
        return alertCounter;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public Optional<Map<String, String>> getGcmDetails() {
        return gcmDetails;
    }

    public Optional<Map<String, String>> getApnsDetails() {
        return apnsDetails;
    }

    public String getMessage() {
        return message;
    }

    public String asJson() {
        JSONObject json = new JSONObject();

        json.put("email", userId);
        json.put("message", message);
        json.put("activity", activity);
        json.put("details", details);
        if (alertCounter.isPresent()) {
            json.put("alertCounter", alertCounter.get());
        }

        if (gcmDetails.isPresent() && gcmDetails.get().size() > 0) {
            JSONObject gcmDetails = new JSONObject();
            for (Map.Entry<String, String> entry : this.gcmDetails.get().entrySet()) {
                gcmDetails.put(entry.getKey(), entry.getValue());
            }
            json.put("gcmDetails", gcmDetails);
        }

        if (apnsDetails.isPresent() && apnsDetails.get().size() > 0) {
            JSONObject apnsDetails = new JSONObject();
            for (Map.Entry<String, String> entry : this.apnsDetails.get().entrySet()) {
                apnsDetails.put(entry.getKey(), entry.getValue());
            }
            json.put("apnsDetails", apnsDetails);
        }

        return json.toString();
    }
}