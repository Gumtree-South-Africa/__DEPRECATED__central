package com.ecg.replyts.integration.smtp;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import org.springframework.beans.factory.annotation.Autowired;

public class CapturingMailDeliveryService implements MailDeliveryService {

    private final MailDeliveryService service;
    private Mail lastSentMail = null;

    @Autowired
    public CapturingMailDeliveryService(MailDeliveryService service) {
        this.service = service;
    }

    @Override
    public void deliverMail(Mail m) throws MailDeliveryException {
        service.deliverMail(m);
        lastSentMail = m;
    }

    public Mail getLastSentMail() {
        return lastSentMail;
    }

}
