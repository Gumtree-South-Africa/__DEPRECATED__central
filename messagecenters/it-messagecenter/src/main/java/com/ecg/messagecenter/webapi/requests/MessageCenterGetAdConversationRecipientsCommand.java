package com.ecg.messagecenter.webapi.requests;

import com.ecg.replyts.core.api.webapi.Method;
import com.ecg.replyts.core.api.webapi.commands.TypedCommand;
import com.google.common.base.Optional;

/**
 * @author elvalencia
 */
public class MessageCenterGetAdConversationRecipientsCommand implements TypedCommand {

    public static final String MAPPING = "/postboxes/{urlEncodedSellerEmail}/ad/{adId}/buyeremails";

    private final String urlEncodedSellerEmail;
    private final String adId;

    public MessageCenterGetAdConversationRecipientsCommand(String urlEncodedSellerEmail,
                    String adId) {
        this.urlEncodedSellerEmail = urlEncodedSellerEmail;
        this.adId = adId;
    }

    @Override public Method method() {
        return Method.GET;
    }

    @Override public String url() {
        return "/postboxes/" + urlEncodedSellerEmail + "/ad/" + adId + "/buyeremails";
    }

    @Override public Optional<String> jsonPayload() {
        return Optional.absent();
    }
}