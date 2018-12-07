package com.ecg.comaas.kjca.coremod.shared;

import com.ecg.replyts.core.api.model.conversation.Conversation;

public class AddresserUtil {

    private static final String ANONYMIZE_CONVO_KEY = BoxHeaders.ANONYMIZE.getCustomConversationValueName().get();

    public static boolean shouldAnonymizeConversation(Conversation conversation) {
        String anonymityLevel = conversation.getCustomValues().get(ANONYMIZE_CONVO_KEY);

        if (anonymityLevel == null) {
            // If the conversation doesn't have the header (very rare Riak / merge conversation bug),
            // error on the side of caution and anonymize.
            return true;
        }

        switch (anonymityLevel.toLowerCase()) {
            case "false":
                return false;
            default:
                // I'm not sure what other levels there are
                return true;
        }
    }
}
