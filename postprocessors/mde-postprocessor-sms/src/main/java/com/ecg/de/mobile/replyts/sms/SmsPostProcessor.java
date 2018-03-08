package com.ecg.de.mobile.replyts.sms;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;

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
        Date sentDate = messageContext.getMail().map(Mail::getSentDate).orElse(null);
        ContactMessage contactMessage = ContactMessageAssembler.assemble(messageContext.getConversation().getBuyerId(), sentDate, messageContext.getMessage().getHeaders());

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
