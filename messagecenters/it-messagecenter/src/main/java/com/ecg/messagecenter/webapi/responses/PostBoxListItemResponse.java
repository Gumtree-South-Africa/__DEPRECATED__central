package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.util.ConversationBoundnessFinder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.util.MessagesDiffer;
import com.ecg.messagecenter.util.MessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * User: maldana
 * Date: 30.10.13
 * Time: 17:14
 *
 * @author maldana@ebay.de
 */
public class PostBoxListItemResponse {

    private Logger LOG = LoggerFactory.getLogger(getClass());

    private ConversationState state;
    private String email;
    private String id;
    private Long negotiationId;
    private String buyerName;
    private String sellerName;
    private String buyerEmail;
    private String sellerEmail;
    private String adId;

    private ConversationRole role;
    private boolean unread;
    private List<String> attachments = Collections.emptyList();

    private MessageResponse lastMessage;

    private PostBoxListItemResponse() {
    }

    public PostBoxListItemResponse(String email, ConversationThread conversationThread,
                    Conversation conversation) {
        Preconditions.checkArgument(conversationThread.containsNewListAggregateData(),
                        "Only supported for data stored as list-aggregate");

        this.email = email;
        this.unread = conversationThread.isContainsUnreadMessages();
        this.id = conversationThread.getConversationId();
        this.buyerName = conversationThread.getBuyerName().isPresent() ?
                        conversationThread.getBuyerName().get() :
                        "";
        this.sellerName = conversationThread.getSellerName().isPresent() ?
                        conversationThread.getSellerName().get() :
                        "";
        this.adId = conversationThread.getAdId();
        this.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationThread);
        logConversationThread(conversationThread);
        if (conversation != null) {
            this.buyerEmail = conversation.getBuyerId();
            this.sellerEmail = conversation.getSellerId();
            this.state = conversation.getState();
            logConversation(conversation);
            if (!hasName(buyerName)) {
                buyerName = getName(conversation.getMessages(), MessageDirection.BUYER_TO_SELLER);
            }
            if (!hasName(sellerName)) {
                sellerName = getName(conversation.getMessages(), MessageDirection.SELLER_TO_BUYER);
            }
        }

        this.lastMessage = new MessageResponse(MessageCenterUtils
                        .toFormattedTimeISO8601ExplicitTimezoneOffset(
                                        conversationThread.getReceivedAt()),
                        conversationThread.getOfferId(), conversationThread.getRobot(),
                        ConversationBoundnessFinder.boundnessForRole(this.role,
                                        conversationThread.getMessageDirection().get()),
                        conversationThread.getPreviewLastMessage().get(), Optional.empty(),
                        Collections.<MessageResponse.Attachment>emptyList(), null, "");
    }

    private void logConversationThread(ConversationThread conversationThread) {
        try {
            LOG.debug("ConversationThread: " + new ObjectMapper()
                            .writeValueAsString(conversationThread));
            if (conversationThread.getBuyerId().isPresent())
                LOG.debug("getBuyerId: " + conversationThread.getBuyerId().get());
            if (conversationThread.getBuyerName().isPresent())
                LOG.debug("getBuyerName: " + conversationThread.getBuyerName().get());
            if (conversationThread.getMessageDirection().isPresent())
                LOG.debug("getMessageDirection: " + conversationThread.getMessageDirection().get());
            if (conversationThread.getPreviewLastMessage().isPresent())
                LOG.debug("getPreviewLastMessage: " + conversationThread.getPreviewLastMessage()
                                .get());
            if (conversationThread.getSellerName().isPresent())
                LOG.debug("getSellerName: " + conversationThread.getSellerName().get());
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    private void logConversation(Conversation conversation) {
        try {
            LOG.debug("Convesation: " + new ObjectMapper().writeValueAsString(conversation));
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    private boolean hasName(String name) {
        return ResponseUtil.hasName(name);
    }

    private String getName(List<Message> messages, MessageDirection direction) {
        return ResponseUtil.getName(messages, direction);
    }

    // old style lookup when we didn't have a complete search aggregate on the list-view
    // todo: marked on calendar around June we deprecated this and throw it out (work with '...' placeholders if not data available)
    @Deprecated public static Optional<PostBoxListItemResponse> createNonAggregateListViewItem(
                    String email, boolean isUnread, Conversation conversationRts) {
        PostBoxListItemResponse response = new PostBoxListItemResponse();
        response.email = email;
        response.unread = isUnread;
        response.id = conversationRts.getId();
        response.buyerName = ResponseUtil.getSecureName(
                        conversationRts.getCustomValues().get("buyer-name") == null ?
                                        "" :
                                        conversationRts.getCustomValues().get("buyer-name"));
        response.buyerEmail = conversationRts.getBuyerId();
        response.sellerName = ResponseUtil.getSecureName(
                        conversationRts.getCustomValues().get("seller-name") == null ?
                                        "" :
                                        conversationRts.getCustomValues().get("seller-name"));
        response.sellerEmail = conversationRts.getSellerId();
        response.state = conversationRts.getState();
        response.adId = conversationRts.getAdId();
        response.role = ConversationBoundnessFinder.lookupUsersRole(email, conversationRts);

        MessagesResponseFactory factory = new MessagesResponseFactory(new MessagesDiffer());
        Optional<MessageResponse> messageResponse = factory.latestMessage(email, conversationRts);
        if (messageResponse.isPresent()) {
            response.lastMessage = messageResponse.get();
            return Optional.of(response);

        } else {
            return Optional.empty();
        }
    }

    public String getEmail() {
        return email;
    }

    public String getId() {
        return id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public ConversationState getState() {
        return state;
    }

    public String getAdId() {
        return adId;
    }

    public String getReceivedDate() {
        return lastMessage.getReceivedDate();
    }

    public String getSenderEmail() {
        return lastMessage.getSenderEmail();
    }

    public ConversationRole getRole() {
        return role;
    }

    public MailTypeRts getBoundness() {
        return lastMessage.getBoundness();
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getTextShortTrimmed() {
        return lastMessage.getTextShortTrimmed();
    }

    public boolean isUnread() {
        return unread;
    }

    public List<String> getAttachments() {
        return attachments == null ? Collections.<String>emptyList() : attachments;
    }

    public String getRobot() {
        return lastMessage.getRobot();
    }

    public String getOfferId() {
        return lastMessage.getOfferId();
    }
}
