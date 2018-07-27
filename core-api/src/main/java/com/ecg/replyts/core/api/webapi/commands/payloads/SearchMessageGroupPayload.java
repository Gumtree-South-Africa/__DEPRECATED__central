package com.ecg.replyts.core.api.webapi.commands.payloads;

public class SearchMessageGroupPayload extends SearchMessagePayload {

    public static final String FROM_EMAIL_ES_FIELDNAME = "fromEmail";

    private String groupBy;

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getGroupBy() {
        return groupBy;
    }
}
