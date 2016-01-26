package com.ecg.replyts.core.api.search;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class RtsSearchGroupResponse {
    private final Map<String, RtsSearchResponse> messageGroups;

    public RtsSearchGroupResponse(Map<String, RtsSearchResponse> messageGroups) {
        this.messageGroups = ImmutableMap.copyOf(messageGroups);
    }

    public Map<String, RtsSearchResponse> getMessageGroups() {
        return messageGroups;
    }
}
