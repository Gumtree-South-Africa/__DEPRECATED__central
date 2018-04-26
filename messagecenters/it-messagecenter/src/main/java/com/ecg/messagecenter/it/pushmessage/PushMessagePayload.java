package com.ecg.messagecenter.it.pushmessage;

import com.google.common.collect.Maps;
import net.sf.json.JSONObject;

import java.util.Map;
import java.util.Optional;

public class PushMessagePayload {

    private final String email;
    private final String message;
    private final String activity;
    private final Optional<Integer> alertCounter;
    private final Map<String, String> details;
    private final Optional<Map<String, String>> gcmDetails;
    private final Optional<Map<String, String>> apnsDetails;
    private final Optional<Map<String, String>> notificationsDetails;

    public PushMessagePayload(String email, String message, String activity,
                    Map<String, String> details) {
        this(email, message, activity, details, Optional.<Integer>empty(),
                        Optional.<Map<String, String>>empty(),
                        Optional.<Map<String, String>>empty(),
                        Optional.<Map<String, String>>empty());
    }

    public PushMessagePayload(String email, String message, String activity,
                    Map<String, String> details, Optional<Integer> alertCounter,
                    Optional<Map<String, String>> gcmDetails,
                    Optional<Map<String, String>> apnsDetails,
                    Optional<Map<String, String>> notificationsDetails) {
        if (details == null) {
            details = Maps.newHashMap();
        }
        this.gcmDetails = gcmDetails;
        this.apnsDetails = apnsDetails;
        this.email = email;
        this.message = message;
        this.activity = activity;
        this.details = details;
        this.alertCounter = alertCounter;
        this.notificationsDetails = notificationsDetails;
    }

    public String getEmail() {
        return email;
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

        if (notificationsDetails.isPresent() && notificationsDetails.get().size() > 0) {
            JSONObject apnsDetails = new JSONObject();
            for (Map.Entry<String, String> entry : this.notificationsDetails.get().entrySet()) {
                apnsDetails.put(entry.getKey(), entry.getValue());
            }
            json.put("notificationsDetails", apnsDetails);
        }

        return json.toString();
    }
}