package com.ecg.messagecenter.gtau.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class RichMessage {

    private final String richMessageText;
    private final List<Link> links;

    public RichMessage(String richMessageText, List<Entities.MessageLinkInfo> messageLinks) {
        this.richMessageText = richMessageText;
        this.links = messageLinks == null ? Collections.emptyList() : messageLinks.stream().map(Link::new).collect(Collectors.toList());
    }

    public String getRichMessageText() {
        return richMessageText;
    }

    public List<Link> getLinks() {
        return links;
    }
}
