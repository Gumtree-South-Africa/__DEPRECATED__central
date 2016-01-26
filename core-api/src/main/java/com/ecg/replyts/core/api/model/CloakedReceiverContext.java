package com.ecg.replyts.core.api.model;


import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;

/**
 * Record of an anonymized user. An anonymized user is a reference to a role in a specific conversation. Each anonymized user identifies one person in a conversation.
 * The other way round, each conversation has two anonymized user: one who is the buyer and one who is the seller.
 */
public class CloakedReceiverContext {

    private MutableConversation conversation;
    private ConversationRole role;

    public CloakedReceiverContext(MutableConversation conversation, ConversationRole conversationRole) {
        this.role = conversationRole;
        this.conversation = conversation;
    }


    /**
     * returns the id of the conversation this user points to
     *
     * @return id of associated conv
     */
    public MutableConversation getConversation() {
        return conversation;
    }

    /**
     * @return user's role in the conversation
     */
    public ConversationRole getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CloakedReceiverContext that = (CloakedReceiverContext) o;


        if (role != that.role) return false;

        if (this.conversation != null && that.conversation != null) {
            return this.conversation.getId().equals(that.conversation.getId());
        }
        return this.conversation == null && that.conversation == null;
    }

    @Override
    public int hashCode() {
        int result = conversation != null ? conversation.getId().hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}
