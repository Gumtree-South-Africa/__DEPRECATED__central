package com.ecg.replyts.core.api.pluginconfiguration.filter;

import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;


public interface IpFilter extends Filter {
    String IP_ADDRESS_METADATA_KEY = "ip-address";

    // TODO akobiakov: the code below should go away as a whole: https://jira.corp.ebay.com/browse/COMAAS-1695
    default String getIp(MessageProcessingContext ctx, String headerName) {
        if (ctx.getTransport() == MessageTransport.CHAT
                && ctx.getMessage().getCaseInsensitiveHeaders().containsKey(IP_ADDRESS_METADATA_KEY)) {
            return ctx.getMessage().getCaseInsensitiveHeaders().get(IP_ADDRESS_METADATA_KEY);
        }
        return ctx.getMail()
                .map(Mail::getUniqueHeaders)
                .orElseGet(() -> ctx.getMessage().getCaseInsensitiveHeaders())
                .get(headerName);
    }
}
