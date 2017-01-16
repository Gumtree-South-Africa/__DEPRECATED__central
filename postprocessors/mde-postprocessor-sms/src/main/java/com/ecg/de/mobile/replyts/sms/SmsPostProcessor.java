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
    public void postProcess( MessageProcessingContext messageContext) {
    	String adIdString = messageContext.getConversation()
				.getAdId();
    	long adId = Utils.adIdStringToAdId(adIdString);
    	ContactMessage contactMessage = ContactMessageAssembler.assemble(messageContext.getConversation().getBuyerId(), messageContext.getMail().getSentDate(), messageContext.getMessage().getHeaders());
    	
    	LOG.debug("SmsPostProcessor contactMessage " + contactMessage);
    	LOG.info("sendSms " + sendSms(adId, contactMessage));
    }

    
    private boolean sendSms(long adId, ContactMessage contactMessage) {
        // if sms sending fails just log it
        if (StringUtils.hasText(contactMessage.getSmsPhoneNumber())) {
            try {
            	contactMessageSmsService.send(adId, contactMessage);
                return true;
            } catch (Exception e) {
            	LOG.error("Could not send sms.", e);
            }
        }

        return false;
    }
}
