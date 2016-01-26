package com.ecg.replyts.core.api.model.conversation;

/**
 * Role of a user in a {@link Conversation}.
 */
public enum ConversationRole {
    Buyer("buyer"), Seller("seller");

    private String roleStringRepresentation;

    private ConversationRole(String roleName) {
        this.roleStringRepresentation = roleName;
    }

    public boolean matches(String roleName) {
        return roleName != null && roleName.equalsIgnoreCase(this.roleStringRepresentation);
    }

    public ConversationRole otherRole() {
        return this == Buyer ? Seller : Buyer;
    }

    public static ConversationRole getRole(String mail, Conversation conv) {
        return conv.getBuyerId().equalsIgnoreCase(mail) ? Buyer : Seller;
    }
}