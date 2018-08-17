package com.ecg.replyts.core.api.model.conversation;

import com.ecg.comaas.events.Conversation.Participant.Role;

/**
 * Role of a user in a {@link Conversation}.
 */
public enum ConversationRole {
    Buyer("buyer", Role.BUYER), Seller("seller", Role.SELLER);

    private final String roleStringRepresentation;
    private final Role participantRole;

    ConversationRole(String roleName, Role participantRole) {
        this.roleStringRepresentation = roleName;
        this.participantRole = participantRole;
    }

    public boolean matches(String roleName) {
        return roleName != null && roleName.equalsIgnoreCase(this.roleStringRepresentation);
    }

    public static ConversationRole getRole(String mail, Conversation conv) {
        return conv.getBuyerId().equalsIgnoreCase(mail) ? Buyer : Seller;
    }

    public Role getParticipantRole() {
        return participantRole;
    }
}