package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;

import java.util.Map;

class ConversationStartInfo {

    private final MessageProcessingContext ctx;

    public ConversationStartInfo(MessageProcessingContext ctx) {
        this.ctx = ctx;
    }

    public MailAddress buyer() {
        Mail m = ctx.getMail();
        String intendedFrom = Strings.isNullOrEmpty(m.getReplyTo()) ? m.getFrom() : m.getReplyTo();
        return new MailAddress(intendedFrom);
    }

    public MailAddress seller() {
        return new MailAddress(ctx.getMail().getDeliveredTo());
    }

    public String adId() {
        return ctx.getMail().getAdId();
    }

    public Map<String, String> customHeaders() {
        return ctx.getMail().getCustomHeaders();
    }

    public ConversationIndexKey asConversationIndexKeyBuyerToSeller() {
        return new ConversationIndexKey(buyer().getAddress(), seller().getAddress(), adId());
    }

    public ConversationIndexKey asConversationIndexKeySellerToBuyer() {
        return new ConversationIndexKey(seller().getAddress(), buyer().getAddress(), adId());
    }
}
