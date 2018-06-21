package com.ecg.replyts.core.api.pluginconfiguration.filter;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

public interface IpFilter extends Filter {
    default String getIp(MessageProcessingContext context, String headerName) {
        return context.getMail()
                .map(Mail::getUniqueHeaders)
                .orElseGet(() -> context.getMessage().getHeaders())
                .get(headerName);
    }
}
