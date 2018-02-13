package com.ebay.ecg.bolt.domain.service.push.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PushMessagePayload {
    @JsonSerialize
    @JsonDeserialize
    private final String email;
    
    @JsonSerialize
    @JsonDeserialize
    private final String message;
    
    @JsonSerialize
    @JsonDeserialize
    private final String activity;

    @JsonSerialize
    @JsonDeserialize
    private final Optional<Integer> alertCounter;
    
    @JsonSerialize
    @JsonDeserialize
    private final Map<String, String> details;
    
    @JsonSerialize
    @JsonDeserialize
    private final Optional<Map<String, String>> gcmDetails;
    
    @JsonSerialize
    @JsonDeserialize
    private final Optional<Map<String, String>> apnsDetails;

    public PushMessagePayload(String email, String message, String activity, Map<String, String> details) {
        this(email, message, activity, details, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public PushMessagePayload(String email, String message, String activity, Map<String, String> details, Optional<Integer> alertCounter, Optional<Map<String, String>> gcmDetails, Optional<Map<String, String>> apnsDetails) {
        if (details == null) {
            details = new HashMap<>();
        }

        this.gcmDetails = gcmDetails;
        this.apnsDetails = apnsDetails;
        this.email = email;
        this.message = message;
        this.activity = activity;
        this.details = details;
        this.alertCounter = alertCounter;
    }

    public String getEmail() {
        return email;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public String getMessage() {
        return message;
    }

    public String getActivity() {
        return activity;
    }

    public Optional<Integer> getAlertCounter() {
        return alertCounter;
    }

    @Override
    public String toString() {
        return "PushMessagePayload [email=" + email + ", message=" + message
          + ", activity=" + activity + ", alertCounter=" + alertCounter
          + ", details=" + details + ", gcmDetails=" + gcmDetails
          + ", apnsDetails=" + apnsDetails + "]";
    }

    public String asJson() {
        JSONObject json = new JSONObject();

        json.put("email", email);
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