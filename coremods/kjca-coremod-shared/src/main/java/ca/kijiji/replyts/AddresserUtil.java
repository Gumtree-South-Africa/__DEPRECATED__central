package ca.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.Conversation;

public class AddresserUtil {

    private static final String ANONYMIZE_CONVO_KEY = BoxHeaders.ANONYMIZE.getCustomConversationValueName().get();

    public static boolean shouldAnonymizeConversation(Conversation conversation) {
        String convoLevelAnon = conversation.getCustomValues().get(ANONYMIZE_CONVO_KEY);

        if (convoLevelAnon != null) {
            return !"false".equalsIgnoreCase(convoLevelAnon);
        }

        // If the conversation doesn't have the header (very rare Riak / merge conversation bug),
        // error on the side of caution and anonymize.
        return true;
    }
}
