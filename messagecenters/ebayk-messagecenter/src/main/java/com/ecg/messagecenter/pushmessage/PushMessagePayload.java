package com.ecg.messagecenter.pushmessage;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.collect.ImmutableMap;
import net.sf.json.JSONObject;

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

    PushMessagePayload(String email, String message, String activity, Map<String, String> details) {
        this(email, message, activity, details, Optional.<Integer>empty(), Optional.<Map<String, String>>empty(), Optional.<Map<String, String>>empty());
    }

    PushMessagePayload(String email, String message, String activity, Map<String, String> details, Optional<Integer> alertCounter, Optional<Map<String, String>> gcmDetails, Optional<Map<String, String>> apnsDetails) {
        ImmutableMap.Builder<String, String> detailsBuilder = new ImmutableMap.Builder<String, String>();
        if (details != null) {
            detailsBuilder.putAll(details);
        }
        this.gcmDetails = gcmDetails;
        this.apnsDetails = apnsDetails;
        this.email = email;
        this.message = message;
        this.activity = activity;
        this.alertCounter = alertCounter;

        String pushNotificationId = uuidGenerator.generate().toString();

        this.details = constructUtmDetails(detailsBuilder, pushNotificationId);
    }

    private Map<String, String> constructUtmDetails(ImmutableMap.Builder<String, String> detailsBuilder, String pushNotificationId) {
        detailsBuilder.put("utm_source", "System");
        detailsBuilder.put("utm_medium", "PushNotification");
        detailsBuilder.put("utm_campaign", "NewMessage");
        detailsBuilder.put("utm_content", pushNotificationId);
        return detailsBuilder.build();
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

    JSONObject asJson() {
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
        return json;
    }
}
