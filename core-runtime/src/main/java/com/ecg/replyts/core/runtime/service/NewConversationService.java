package com.ecg.replyts.core.runtime.service;

import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class NewConversationService {

    private final UniqueConversationSecret uniqueConversationSecret;
    private final MutableConversationRepository mutableConversationRepository;

    @Autowired
    public NewConversationService(UniqueConversationSecret uniqueConversationSecret, MutableConversationRepository mutableConversationRepository) {
        this.uniqueConversationSecret = uniqueConversationSecret;
        this.mutableConversationRepository = mutableConversationRepository;
    }

    public void commitConversation(String convId, String adId, String buyerEmail, String sellerEmail, ConversationState conversationState, Map<String, String> customValues) {

        ConversationCreatedEvent conversationCreatedEvent = new ConversationCreatedEvent(
                convId,
                adId,
                buyerEmail,
                sellerEmail,
                nextSecret(),
                nextSecret(),
                new DateTime(),
                conversationState,
                customValues
        );

        mutableConversationRepository.commit(convId, Collections.singletonList(conversationCreatedEvent));
    }

    public String nextGuid() {
        return Guids.next();
    }
    
    public String nextSecret() {
        return uniqueConversationSecret.nextSecret();
    }
}
