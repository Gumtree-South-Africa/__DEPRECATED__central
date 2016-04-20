package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.UnsignedLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
class UniqueConversationSecret {

    private static final Random RANDOM = new Random(System.nanoTime());
    private static final int MAX_RETRIES = 10;

    private final ConversationRepository conversationRepository;


    @Autowired
    public UniqueConversationSecret(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public String nextSecret() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            long newSecret = RANDOM.nextLong();
            String hash = UnsignedLong.fromLong(newSecret).toBase30();
            if (conversationRepository.isSecretAvailable(hash)) {
                return hash;
            }
        }
        throw new IllegalStateException("AnonymizedMailGenerator could not generate a valid secret mail address within " + MAX_RETRIES + " retries");
    }

}
