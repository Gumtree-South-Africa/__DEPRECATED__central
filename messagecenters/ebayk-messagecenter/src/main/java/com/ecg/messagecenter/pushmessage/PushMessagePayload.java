package com.ecg.messagecenter.pushmessage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.sf.json.JSONObject;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

import java.util.Map;
import java.util.Optional;

/**
 * User: maldana
 * Date: 18.09.13
 * Time: 17:19
 *
 * @author maldana@ebay.de
 */
public class PushMessagePayload {

    private static final TimeBasedGenerator uuidGenerator = Generators.timeBasedGenerator();
    private final String email;
    private final String message;
    private final String activity;
    private final Optional<Integer> alertCounter;
    private final Map<String, String> details;
    private final Optional<Map<String, String>> gcmDetails;
    private final Optional<Map<String, String>> apnsDetails;
    private final Map<String, String> utmDetails;

    PushMessagePayload(String email, String message, String activity, Map<String, String> details) {
        this(email, message, activity, details, Optional.<Integer>empty(), Optional.<Map<String, String>>empty(), Optional.<Map<String, String>>empty());
    }

    PushMessagePayload(String email, String message, String activity, Map<String, String> details, Optional<Integer> alertCounter, Optional<Map<String, String>> gcmDetails, Optional<Map<String, String>> apnsDetails) {
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

        String pushNotificationId = uuidGenerator.generate().toString();

        this.utmDetails = constructUtmDetails(pushNotificationId);
    }

    private Map<String, String> constructUtmDetails(String pushNotificationId) {
        return ImmutableMap.of("utm_source", "System",
                "utm_medium", "PushNotification",
                "utm_campaign", "NewMessage",
                "utm_content", pushNotificationId);
    }

    public String getEmail() {
        return email;
    }

    String getActivity() {
        return activity;
    }

    Optional<Integer> getAlertCounter() {
        return alertCounter;
    }

    Map<String, String> getDetails() {
        return details;
    }

    Optional<Map<String, String>> getGcmDetails() {
        return gcmDetails;
    }

    Optional<Map<String, String>> getApnsDetails() {
        return apnsDetails;
    }

    public String getMessage() {
        return message;
    }

    Map<String, String> getUtmDetails() {
        return utmDetails;
    }

    String asJson() {
        JSONObject json = new JSONObject();

        json.put("email", email);
        json.put("message", message);
        json.put("activity", activity);
        json.put("details", details);
        if (alertCounter.isPresent()) {
            json.put("alertCounter", alertCounter.get());
        }

        if (gcmDetails.isPresent() && !gcmDetails.get().isEmpty()) {
            JSONObject gcmDetails = new JSONObject();
            for (Map.Entry<String, String> entry : this.gcmDetails.get().entrySet()) {
                gcmDetails.put(entry.getKey(), entry.getValue());
            }
            json.put("gcmDetails", gcmDetails);
        }

        if (apnsDetails.isPresent() && !apnsDetails.get().isEmpty()) {
            JSONObject apnsDetails = new JSONObject();
            for (Map.Entry<String, String> entry : this.apnsDetails.get().entrySet()) {
                apnsDetails.put(entry.getKey(), entry.getValue());
            }
            json.put("apnsDetails", apnsDetails);
        }
        json.put("utmDetails", utmDetails);

        return json.toString();
    }

}
