package com.ebay.ecg.replyts.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;

public final class Link {

    private final String url;
    private final String type;
    private final int start;
    private final int end;

    public Link(String url, String type, int start, int end) {
        this.url = url;
        this.type = type;
        this.start = start;
        this.end = end;
    }

    public Link(Entities.MessageLinkInfo messageLinkInfo) {
        this(messageLinkInfo.getUrl(), messageLinkInfo.getType().name(), messageLinkInfo.getBeginIndex(), messageLinkInfo.getEndIndex());
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
