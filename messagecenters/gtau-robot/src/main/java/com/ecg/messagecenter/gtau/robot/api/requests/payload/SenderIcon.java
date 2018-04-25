package com.ecg.messagecenter.gtau.robot.api.requests.payload;

import com.ebay.ecg.australia.events.entity.Entities;

public final class SenderIcon {

    private final String name;
    private final String url;

    public SenderIcon(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public SenderIcon(Entities.MessageSenderIcon icon) {
        this(icon.getName(), icon.getSource());
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
