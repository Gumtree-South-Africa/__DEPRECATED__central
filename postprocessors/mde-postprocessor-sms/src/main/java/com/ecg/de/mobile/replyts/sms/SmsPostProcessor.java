package com.ecg.de.mobile.replyts.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.util.StringUtils;

public class SmsPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SmsPostProcessor.class);

    private final ContactMessageSmsService contactMessageSmsService;
    private final int order;

    SmsPostProcessor(ContactMessageSmsService contactMessageSmsService, int order) {
        this.contactMessageSmsService = contactMessageSmsService;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void postProcess(MessageProcessingContext messageContext) {
        ContactMessage contactMessage = ContactMessageAssembler.assemble(messageContext.getConversation().getBuyerId(), messageContext.getMail().getSentDate(), messageContext.getMessage().getHeaders());

        LOG.trace("SmsPostProcessor contactMessage {}", contactMessage);
        boolean result = sendSms(contactMessage);
        LOG.trace("sendSms {}", result);
    }


    private boolean sendSms(ContactMessage contactMessage) {
        // if sms sending fails just log it
        if (StringUtils.hasText(contactMessage.getSmsPhoneNumber())) {
            try {
              return contactMessageSmsService.send(contactMessage);
            } catch (Exception e) {
                LOG.error("Could not send sms.", e);
            }
        }

        return false;
    }
}
