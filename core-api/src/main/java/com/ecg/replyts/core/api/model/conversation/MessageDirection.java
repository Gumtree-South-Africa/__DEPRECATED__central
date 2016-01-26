package com.ecg.replyts.core.api.model.conversation;

/**
 * The direction of a {@link Message}, from buyer to seller or vice versa.
 */
public enum MessageDirection {

    /**
     * Receiver of this mail is the seller in the conversation.
     */
    BUYER_TO_SELLER(ConversationRole.Buyer, ConversationRole.Seller),
    /**
     * Receiver of this mail is the buyer in the conversation.
     */
    SELLER_TO_BUYER(ConversationRole.Seller, ConversationRole.Buyer),
    /**
     * undefined message direction for when the mail was unparsable or the cloaked mail format was illegal.
     */
    UNKNOWN(null, null);

    private final ConversationRole fromRole;
    private final ConversationRole toRole;

    private MessageDirection(ConversationRole from, ConversationRole to) {
        this.fromRole = from;
        this.toRole = to;
    }

    public ConversationRole getFromRole() {
        return fromRole;
    }

    public ConversationRole getToRole() {
        return toRole;
    }

    public MessageDirection other() {
        return (this == BUYER_TO_SELLER) ? SELLER_TO_BUYER : BUYER_TO_SELLER;
    }

    public static MessageDirection getWithToRole(ConversationRole toRole) {
        return toRole == ConversationRole.Buyer ? SELLER_TO_BUYER : BUYER_TO_SELLER;
    }

}
