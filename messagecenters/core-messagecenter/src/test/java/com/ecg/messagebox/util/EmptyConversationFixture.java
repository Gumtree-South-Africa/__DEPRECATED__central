package com.ecg.messagebox.util;

import com.ecg.messagebox.controllers.requests.EmptyConversationRequest;
import com.ecg.messagebox.model.*;

import java.util.HashMap;
import java.util.Map;

import static com.datastax.driver.core.utils.UUIDs.timeBased;

public final class EmptyConversationFixture {

    public static final String ADVERT_ID = "AD1";
    public static final String BUYER_ID_1 = "BUYER_ID_1";
    public static final String BUYER_NAME = "buyer";
    public static final String SELLER_NAME = "seller";
    public static final String SELLER_ID_1 = "SELLER_ID_1";
    public static final String BUYER_EMAIL_1 = "buyer@martkplaats.nl";
    public static final String SELLER_EMAIL_1 = "seller@martkplaats.nl";
    public static final String AD_TITLE = "Test AD";
    public static final String EMAIL_SUBJECT = "Test subject";



    public static EmptyConversationRequest validEmptyConversation() {
        return validEmptyConversationRequest(ADVERT_ID, BUYER_ID_1, SELLER_ID_1);
    }

    public static EmptyConversationRequest validEmptyConversationRequest(final String ADVERT_ID, final String BUYER_ID_1, final String SELLER_ID_1) {

        EmptyConversationRequest emptyConversationRequest = new EmptyConversationRequest();
        emptyConversationRequest.setSenderId(BUYER_ID_1);
        emptyConversationRequest.setAdId(ADVERT_ID);
        emptyConversationRequest.setAdTitle(AD_TITLE);
        emptyConversationRequest.setEmailSubject(EMAIL_SUBJECT);

        Map<String, String> customValues = new HashMap<>();
        customValues.put("from-userid", BUYER_ID_1);
        customValues.put("to-userid", SELLER_ID_1);
        customValues.put("from", BUYER_NAME);
        customValues.put("to", SELLER_NAME);
        customValues.put("buyer-name", BUYER_NAME);
        customValues.put("seller-name", SELLER_NAME);

        emptyConversationRequest.setCustomValues(customValues);

        Message message = new Message(timeBased(), MessageType.CHAT, new MessageMetadata(AD_TITLE, BUYER_ID_1));
        emptyConversationRequest.setMessage(message);

        Participant buyer = new Participant(BUYER_ID_1, BUYER_NAME, BUYER_EMAIL_1, ParticipantRole.BUYER);
        Participant seller = new Participant(SELLER_ID_1, SELLER_NAME, SELLER_EMAIL_1, ParticipantRole.SELLER);

        Map<ParticipantRole, Participant> participantMap = new HashMap();
        participantMap.put(ParticipantRole.BUYER, buyer);
        participantMap.put(ParticipantRole.SELLER, seller);

        emptyConversationRequest.setParticipants(participantMap);
        return emptyConversationRequest;
    }

    public static EmptyConversationRequest invalidEmptyConversation() {
        return new EmptyConversationRequest();
    }
}
