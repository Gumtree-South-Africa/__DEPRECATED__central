package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;

import java.util.Map;

public class ConversationStartInfo {
    private final MessageProcessingContext context;

    public ConversationStartInfo(MessageProcessingContext context) {
        this.context = context;
    }

    public MailAddress buyer() {
        Mail m = context.getMail();
        String intendedFrom = Strings.isNullOrEmpty(m.getReplyTo()) ? m.getFrom() : m.getReplyTo();
        return new MailAddress(intendedFrom);
    }

    public MailAddress seller() {
        return new MailAddress(context.getMail().getDeliveredTo());
    }

    public String adId() {
        return context.getMail().getAdId();
    }

    public Map<String, String> customHeaders() {
        return context.getMail().getCustomHeaders();
    }
}
