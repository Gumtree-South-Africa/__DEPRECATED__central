package com.ecg.messagecenter.webapi.responses;

import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Created by elvalencia on 22/07/2015.
 */
public class AdConversationRecipientListResponse {
    private String adId;
    private List<BuyerContactResponse> buyerContacts = Collections.emptyList();

    public AdConversationRecipientListResponse(String adId,
                    List<BuyerContactResponse> buyerContacts) {
        Preconditions.checkNotNull(adId);
        Preconditions.checkNotNull(buyerContacts);
        this.adId = adId;
        this.buyerContacts = buyerContacts;
    }

    public String getAdId() {
        return adId;
    }

    public List<BuyerContactResponse> getBuyerContacts() {
        return buyerContacts;
    }
}
