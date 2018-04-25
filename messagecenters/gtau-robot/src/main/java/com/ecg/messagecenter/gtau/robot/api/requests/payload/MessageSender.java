package com.ecg.messagecenter.gtau.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class MessageSender {

    private final String name;
    private final List<SenderIcon> senderIcons;

    public MessageSender(String name, List<Entities.MessageSenderIcon> icons) {
        this.name = name;
        this.senderIcons = icons == null ? Collections.emptyList() : icons.stream().map(SenderIcon::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<SenderIcon> getSenderIcons() {
        return senderIcons;
    }
}
