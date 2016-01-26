package com.ecg.replyts.core.api.webapi.commands.payloads;

public class SearchMessageGroupPayload extends SearchMessagePayload {
    public static final String FROM_EMAIL_ES_FIELDNAME = "fromEmail";
    public static final String IP_ADDRESS_ES_FIELDNAME = "customHeaders.ip-address";

    private String groupBy;

    public String getGroupBy() {
        return groupBy;
    }
}
